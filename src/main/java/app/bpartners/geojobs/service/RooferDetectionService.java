package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionProcessSucceeded;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RooferDetectionService
    implements Function<app.bpartners.geojobs.repository.model.detection.Detection, Detection> {
  private final TileObjectDetector detector;
  private final DetectionMaskCreator detectionMaskCreator;
  private final DetectionMapper detectionMapper;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final VGGFactory vggFactory;
  private final DetectionRepository detectionRepository;
  private final EventProducer<GeoJsonConversionProcessSucceeded> eventProducer;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final DetectionVGGUpdate detectionVGGUpdate;

  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    var providedGeoJson = detection.getProvidedGeoJsonZone();
    int zoom =
        providedGeoJson.isEmpty() || providedGeoJson.getFirst().getProperties().get("zoom") == null
            ? HOUSES_0.getZoomLevel()
            : (Integer) providedGeoJson.getFirst().getProperties().get("zoom");

    List<List<BigDecimal>> flattedFeatures;
    if (detection.getPolygonRoofDelimitation() != null
        && !detection.getPolygonRoofDelimitation().isEmpty()) {
      flattedFeatures = detection.getPolygonRoofDelimitation();
    } else {
      flattedFeatures =
          providedGeoJson.stream()
              .map(app.bpartners.geojobs.endpoint.rest.model.Feature::getGeometry)
              .filter(Objects::nonNull)
              .map(FeatureGeometry::getMultiPolygon)
              .map(MultiPolygon::getCoordinates)
              .filter(Objects::nonNull)
              .flatMap(List::stream)
              .flatMap(List::stream)
              .flatMap(List::stream)
              .toList();
    }
    var mask = detectionMaskCreator.apply(flattedFeatures);

    var tile =
        Tile.builder()
            .coordinates(new TileCoordinates().x(0).y(0).z(zoom))
            .size(new TileInfoSize().width(1024).height(1024))
            .bucketPath(detection.getImageFileKey())
            .build();
    var toDetect =
        TileDetectionTask.builder()
            .id(randomUUID().toString())
            .jobId(detection.getId())
            .tile(tile)
            .build();
    var detectionResponse =
        detector.apply(toDetect, mask, detection.getDetectableObjectConfigurations());
    var machineDetectedTile =
        detectionMapper.toDetectedTile(detectionResponse, tile, null, null, null);
    machineDetectedTileRepository.save(machineDetectedTile);
    var detectedTile =
        DetectedTile.builder()
            .tile(machineDetectedTile.getTile())
            .detectedObjects(machineDetectedTile.getDetectedObjects())
            .build();

    var roofGeometry = polygon(flattedFeatures);
    var updatedDetectionWithVggKey = processVggConversion(detection, roofGeometry, detectedTile);
    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        updatedDetectionWithVggKey, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  private app.bpartners.geojobs.repository.model.detection.Detection processVggConversion(
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      org.locationtech.jts.geom.Polygon roofGeometry,
      DetectedTile detectedTiles) {
    var vgg = vggFactory.from(roofGeometry, detectedTiles);
    var detectionWithVggFileKey = detectionVGGUpdate.apply(vgg, detection);
    var savedDetection = detectionRepository.save(detectionWithVggFileKey);

    eventProducer.accept(
        List.of(GeoJsonConversionProcessSucceeded.builder().detection(savedDetection).build()));

    return savedDetection;
  }

  private org.locationtech.jts.geom.Polygon polygon(List<List<BigDecimal>> features) {
    Coordinate[] coordinates =
        features.stream()
            .map(
                point ->
                    new Coordinate(point.getFirst().doubleValue(), point.getLast().doubleValue()))
            .toArray(Coordinate[]::new);

    if (!coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
      Coordinate[] closedRingCoords = Arrays.copyOf(coordinates, coordinates.length + 1);
      closedRingCoords[closedRingCoords.length - 1] = closedRingCoords[0];
      coordinates = closedRingCoords;
    }

    LinearRing shell = geometryFactory.createLinearRing(coordinates);
    return geometryFactory.createPolygon(shell);
  }
}
