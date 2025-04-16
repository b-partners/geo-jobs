package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionProcessSucceeded;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RooferDetectionService
    implements Function<app.bpartners.geojobs.repository.model.detection.Detection, Detection> {
  private final TileObjectDetector detector;
  private final DetectionMaskCreator detectionMaskCreator;
  private final DetectionMapper detectionMapper;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeoJsonConverter geoJsonConverter;
  private final FileWriter fileWriter;
  private final DetectionRepository detectionRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<GeoJsonConversionProcessSucceeded> eventProducer;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;

  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    var providedGeoJson = detection.getProvidedGeoJsonZone();
    int zoom = providedGeoJson.getFirst().getZoom();
    var flattedFeatures =
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
    var mask = detectionMaskCreator.apply(flattedFeatures);

    var tile =
        Tile.builder()
            .coordinates(new TileCoordinates().x(0).y(0).z(zoom))
            .bucketPath(null)
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
        detectionMapper.toDetectedTile(detectionResponse, tile, null, detection.getId(), null);
    machineDetectedTileRepository.save(machineDetectedTile);
    var detectedTile =
        DetectedTile.builder()
            .tile(machineDetectedTile.getTile())
            .detectedObjects(machineDetectedTile.getDetectedObjects())
            .build();

    processGeoJsonConversion(detection, List.of(detectedTile));
    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  private void processGeoJsonConversion(
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      List<DetectedTile> detectedTiles) {
    var zdjId = detection.getId();
    var geoJson = geoJsonConverter.convert(detectedTiles);
    var zoneName = detection.getZoneName();
    var fileKey = GEO_JSON_BUCKET_FOLDER + zdjId + "/" + zoneName + GEO_JSON_EXTENSION;
    var geoJsonAsByte = geoJson.getStringValue().getBytes();
    var geoJsonAsFile =
        fileWriter.write(geoJsonAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
    bucketComponent.upload(geoJsonAsFile, fileKey);
    detection.setGeojsonS3FileKey(fileKey);
    detectionRepository.save(detection);

    eventProducer.accept(
        List.of(GeoJsonConversionProcessSucceeded.builder().detection(detection).build()));
  }
}
