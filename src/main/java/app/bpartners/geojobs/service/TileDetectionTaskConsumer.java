package app.bpartners.geojobs.service;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Override
  public void accept(TileDetectionTask tileDetectionTask) {
    var detectableObjectConfigurations = tileDetectionTask.getDetectableObjectConfigurations();
    var zoneDetectionJobId = tileDetectionTask.getZoneDetectionJobId();
    var parcelJobId = tileDetectionTask.getJobId();
    var address = tileDetectionTask.getAddress();
    var point = tileDetectionTask.getPoint();
    File mask = null;
    var tile = tileDetectionTask.getTile();
    var detection = detectionRepository.findByZdjId(zoneDetectionJobId).orElse(null);
    if (detection != null) {
      var providedGeoJsonZone = detection.getProvidedGeoJsonZone();
      if (providedGeoJsonZone != null
          && providedGeoJsonZone.size() == 1
          && detection.hasToitureModelName()) {
        var geometryType = providedGeoJsonZone.getFirst().getGeometry().getActualInstance();
        List<BigDecimal> coordinates = new ArrayList<>();
        switch (geometryType) {
          case Point p -> {
            var longitude = p.getCoordinates().getFirst();
            var latitude = p.getCoordinates().getLast();
            coordinates.add(longitude);
            coordinates.add(latitude);
          }
          case Polygon polygon -> {
            var centroidCoordinates = geometryConverter.centroidFromGeometry(polygon);
            var longitude = centroidCoordinates.getFirst();
            var latitude = centroidCoordinates.getLast();
            coordinates.add(longitude);
            coordinates.add(latitude);
          }
          case MultiPolygon multiPolygon -> {
            var centroidCoordinates = geometryConverter.centroidFromGeometry(multiPolygon);
            var longitude = centroidCoordinates.getFirst();
            var latitude = centroidCoordinates.getLast();
            coordinates.add(longitude);
            coordinates.add(latitude);
          }
          default -> throw new IllegalStateException("Unexpected value: " + geometryType);
        }

        var roofMultiPolygon = geometryConverter.retrieveNearestRoofMultiPolygon(coordinates);
        mask = maskRetriever.apply(tile, roofMultiPolygon);
      } else {
        log.info(
            "Only unique provided geojson supported for now, otherwise" + " providedGeojson={}",
            providedGeoJsonZone);
      }
    }

    DetectionResponse response =
        objectsDetector.apply(tileDetectionTask, mask, detectableObjectConfigurations);
    MachineDetectedTile machineDetectedTile =
        detectionMapper.toDetectedTile(
            response, tile, tileDetectionTask.getParcelId(), zoneDetectionJobId, parcelJobId);

    if (machineDetectedTile.getDetectedObjects() != null) {
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
    }
    log.info(
        "[DEBUG] TileDetectionTaskCreatedConsumer to save tile {}", machineDetectedTile.describe());
    machineDetectedTileRepository.save(machineDetectedTile);
  }

  public static TileDetectionTask withNewStatus(
      TileDetectionTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (TileDetectionTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
