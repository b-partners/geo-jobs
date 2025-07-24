package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.event.model.ZoneVggRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import app.bpartners.geojobs.service.PolygonCloser;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZoneVggRequestedService implements Consumer<ZoneVggRequested> {
  private final DetectionRepository detectionRepository;
  private final MachineDetectedTileRepository detectedTileRepository;
  private final VGGFactory vggFactory;
  private final GeometryConverter geometryConverter;
  private final TilingTaskRepository tilingTaskRepository;
  private final DetectionVGGUpdate detectionVGGUpdate;
  private final PolygonCloser polygonCloser;

  @Override
  public void accept(ZoneVggRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var zoneDetectionJobIdentifier = detection.getZdjId();
    var zoneTilingJobIdentifier = detection.getZtjId();

    var providedPolygonZone = detection.getPolygonGeoJsonZone();
    var intersectedTileCoordinates = getTileCoordinatesIntersected(zoneTilingJobIdentifier);
    var tiledPixelPolygons = getTiledPixelPolygon(zoneDetectionJobIdentifier, providedPolygonZone);
    var latLonRoofMultiPolygon = retrieveLatLonRoofMultiPolygon(detection, providedPolygonZone);
    var latLonRoofInsideProvidedZone =
        getLatLonRoofMultiPolygon(latLonRoofMultiPolygon, providedPolygonZone);

    var vggMap =
        vggFactory.from(
            tiledPixelPolygons, latLonRoofInsideProvidedZone, intersectedTileCoordinates);

    var newDetection = detectionVGGUpdate.apply(vggMap, detection);

    detectionRepository.save(newDetection);
  }

  private MultiPolygon getLatLonRoofMultiPolygon(
      MultiPolygon latLonRoofMultiPolygon, Feature providedPolygonZone) {
    var latLonRoofInsideProvidedZone =
        latLonRoofMultiPolygon.intersection(
            geometryConverter.apply(
                List.of(providedPolygonZone.getGeometry().getPolygon().getCoordinates())));
    MultiPolygon latLonRoofInsideProvidedZoneMultiPolygon;
    if (latLonRoofInsideProvidedZone instanceof MultiPolygon multiPolygon) {
      latLonRoofInsideProvidedZoneMultiPolygon = multiPolygon;
    } else if (latLonRoofInsideProvidedZone instanceof Polygon polygon) {
      latLonRoofInsideProvidedZoneMultiPolygon =
          geometryFactory.createMultiPolygon(new Polygon[] {polygon});
    } else {
      throw new IllegalStateException(
          "Unable to convert latLonRoofInsideProvidedZone to MultiPolygon : "
              + geometryConverter.writeGeometryAsString(latLonRoofMultiPolygon));
    }
    return latLonRoofInsideProvidedZoneMultiPolygon;
  }

  private List<TileCoordinates> getTileCoordinatesIntersected(String zoneTilingJobIdentifier) {
    var tilingTasks = tilingTaskRepository.findAllByJobId(zoneTilingJobIdentifier);
    return tilingTasks.stream()
        .map(
            parcelTilingTask ->
                parcelTilingTask.getTiles().stream().map(Tile::getCoordinates).toList())
        .flatMap(List::stream)
        .toList();
  }

  private MultiPolygon retrieveLatLonRoofMultiPolygon(
      Detection detection, Feature polygonGeoJsonZone) {
    return detection.getFeatureWithDelimitations().stream()
        .filter(
            featureWithDelimitation ->
                toRestFeature(featureWithDelimitation.feature()).equals(polygonGeoJsonZone))
        .map(
            featureWithDelimitation ->
                featureWithDelimitation.delimitations().stream()
                    .map(
                        feature ->
                            geometryConverter.apply(
                                toRestFeature(feature)
                                    .getGeometry()
                                    .getMultiPolygon()
                                    .getCoordinates()))
                    .toList())
        .flatMap(List::stream)
        .reduce(unifyMultiPolygon())
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Unable to unify roof multiPolygon to request zone VGG for detection.id="
                        + detection.getId()));
  }

  private List<TiledPixelPolygon> getTiledPixelPolygon(
      String zoneDetectionJobIdentifier, Feature polygonGeoJsonZone) {
    var detectedTileList = detectedTileRepository.findAllByZdjJobId(zoneDetectionJobIdentifier);
    return detectedTileList.stream()
        .map(
            detectedTile -> {
              var polygonObjectTypes =
                  detectedTile.getDetectedObjects().stream()
                      .map(
                          detectedObject -> {
                            var polygonPixel =
                                geometryConverter.toPolygon(
                                    detectedObject
                                        .getFeature()
                                        .getGeometry()
                                        .getMultiPolygon()
                                        .getCoordinates());
                            var forcedClosedPolygonPixel = polygonCloser.apply(polygonPixel);
                            return new PolygonObjectType(
                                forcedClosedPolygonPixel, detectedObject.getDetectableObjectType());
                          })
                      .toList();
              var tileCoordinates = detectedTile.getTile().getCoordinates();
              return new TiledPixelPolygon(
                  polygonGeoJsonZone,
                  polygonObjectTypes,
                  tileCoordinates.getX(),
                  tileCoordinates.getY(),
                  tileCoordinates.getZ());
            })
        .toList();
  }
}
