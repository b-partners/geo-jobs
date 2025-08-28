package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskConsumer implements TaskConsumer<TileDetectionTask> {
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final TileObjectDetector objectsDetector;
  private final DetectionMapper detectionMapper;
  private final DetectionRepository detectionRepository;
  private final GeometryConverter geometryConverter;
  private final DetectionMaskFromTileRetriever maskRetriever;
  private final DetectionProvidedZoneUnifier detectionProvidedZoneUnifier;

  @Override
  public void accept(TileDetectionTask tileDetectionTask) {
    var detectableObjectConfigurations = tileDetectionTask.getDetectableObjectConfigurations();
    var zoneDetectionJobId = tileDetectionTask.getZoneDetectionJobId();
    var parcelJobId = tileDetectionTask.getJobId();
    var address = tileDetectionTask.getAddress();
    var point = tileDetectionTask.getPoint();
    var tile = tileDetectionTask.getTile();
    File mask = null;
    List<DetectedObject> detectedRoof = new ArrayList<>();
    var tileCoordinates = tile.getCoordinates();
    var detection = detectionRepository.findByZdjId(zoneDetectionJobId).orElse(null);
    if (detection != null) {
      var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
      if (providedGeoJsonZone != null && detection.hasToitureModelName()) {
        var unifiedProvidedZone = detectionProvidedZoneUnifier.apply(detection);
        var multiPolygonFromTile =
            geometryConverter.getMultiPolygonFromTile(
                tileCoordinates.getX(), tileCoordinates.getY(), tileCoordinates.getZ());
        var featureWithDelimitations = detection.getFeatureWithDelimitations();
        var roofMultiPolygonIntersectedWithTilePolygon =
            featureWithDelimitations.stream()
                .map(FeatureWithDelimitation::delimitations)
                .flatMap(List::stream)
                .filter(
                    roofFeature -> {
                      var geometry =
                          geometryConverter.readGeometryFromString(
                              roofFeature.getGeometry().getActualInstanceStringValue());
                      if (geometry instanceof MultiPolygon roofMultiPolygon) {
                        return multiPolygonFromTile.intersects(roofMultiPolygon);
                      }
                      if (geometry instanceof Polygon roofPolygon) {
                        return multiPolygonFromTile.intersects(roofPolygon);
                      }
                      return false;
                    })
                .toList();
        if (!roofMultiPolygonIntersectedWithTilePolygon.isEmpty()) {
          var maskMultiPolygon =
              roofMultiPolygonIntersectedWithTilePolygon.stream()
                  .map(
                      roofFeature -> {
                        var geometryRoofFromFeature =
                            geometryConverter.readGeometryFromString(
                                roofFeature.getGeometry().getActualInstanceStringValue());
                        var intersection =
                            geometryRoofFromFeature.intersection(multiPolygonFromTile);
                        if (intersection instanceof MultiPolygon roofMultiPolygon) {
                          return roofMultiPolygon;
                        }
                        if (intersection instanceof Polygon roofPolygon) {
                          return geometryFactory.createMultiPolygon(new Polygon[] {roofPolygon});
                        }
                        return null;
                      })
                  .filter(Objects::nonNull)
                  .reduce(unifyMultiPolygon())
                  .map(
                      unifiedMaskMultiPolygon -> {
                        if (unifiedProvidedZone.isEmpty()) {
                          return unifiedMaskMultiPolygon;
                        }
                        var intersectedMaskWithProvidedZone =
                            unifiedProvidedZone.intersection(unifiedMaskMultiPolygon);
                        if (intersectedMaskWithProvidedZone instanceof Polygon polygon) {
                          return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
                        }
                        if (intersectedMaskWithProvidedZone instanceof MultiPolygon multiPolygon) {
                          return multiPolygon;
                        }
                        return null;
                      })
                  .orElse(null);
          if (maskMultiPolygon != null) {
            log.info(
                "Mask coordinates : {} for tileCoordinates {}", maskMultiPolygon, tileCoordinates);
            mask = maskRetriever.apply(tile, maskMultiPolygon);
            detectedRoof =
                getDetectedRoof(
                    detectableObjectConfigurations,
                    tileDetectionTask,
                    mask,
                    tile,
                    zoneDetectionJobId,
                    parcelJobId);

          } else {
            log.info("Any mask retrieved for tileCoordinates {}", tile);
            return;
          }
        } else {
          log.info(
              "Actual multiPolygon retrieved from tile {} not intersecting with any roof"
                  + " multiPolygon",
              multiPolygonFromTile);
          return;
        }
      }
    }

    DetectionResponse response =
        objectsDetector.apply(tileDetectionTask, mask, detectableObjectConfigurations);

    MachineDetectedTile machineDetectedTile =
        detectionMapper.toDetectedTile(
            response, tile, tileDetectionTask.getParcelId(), zoneDetectionJobId, parcelJobId);

    var detectedObjects = machineDetectedTile.getDetectedObjects();

    if (detectedObjects != null) {
      machineDetectedTile
          .getDetectedObjects()
          .forEach(
              detectedObject -> {
                if (address != null) {
                  detectedObject.getFeature().getProperties().put("address", address);
                }
                if (point != null) {
                  try {
                    var pointAsJson =
                        new ObjectMapper().findAndRegisterModules().writeValueAsString(point);
                    detectedObject.getFeature().getProperties().put("point", pointAsJson);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                }
              });

      detectedObjects = new ArrayList<>(detectedObjects);
      detectedObjects.addAll(detectedRoof);
    }

    machineDetectedTile.setDetectedObjects(detectedObjects);
    machineDetectedTileRepository.save(machineDetectedTile);
  }

  private List<DetectedObject> getDetectedRoof(
      List<DetectableObjectConfiguration> detectableObjectConfigurations,
      TileDetectionTask tileDetectionTask,
      File mask,
      Tile tile,
      String zoneDetectionJobId,
      String parcelJobId) {
    var roofConf =
        detectableObjectConfigurations.stream()
            .map(conf -> conf.toBuilder().objectType(DetectableType.BATI_AUTRES).build())
            .map(c -> (DetectableObjectConfiguration) c)
            .toList();
    DetectionResponse roof = objectsDetector.apply(tileDetectionTask, mask, roofConf);
    MachineDetectedTile detectedTile =
        detectionMapper.toDetectedTile(
            roof, tile, tileDetectionTask.getParcelId(), zoneDetectionJobId, parcelJobId);
    return detectedTile.getDetectedObjects();
  }
}
