package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.page.BoundedPageSize.MAX_SIZE;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeoJsonConversionTaskConsumer implements Consumer<GeoJsonConversionTask> {
  public static final String GEO_JSON_EXTENSION = ".geojson";
  public static final String GEO_JSON_BUCKET_FOLDER = "geoJson/";
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final HumanDetectedTileRepository humanDetectedTileRepository;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final GeoJsonConverter geoJsonConverter;
  private final BucketComponent bucketComponent;
  private final FileWriter writer;
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public void accept(GeoJsonConversionTask geoJsonConversionTask) {
    var conversionJobId = geoJsonConversionTask.getJobId();
    var geoJsonConversionJob =
        geoJsonConversionJobRepository
            .findById(conversionJobId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "GeoConversionJob(id=" + conversionJobId + ") not found"));
    var zoneDetectionType = geoJsonConversionJob.getZoneDetectionJobType();
    var zoneDetectionJobId = geoJsonConversionJob.getZoneDetectionJobId();
    var zoneDetectionJob = zoneDetectionJobService.findById(zoneDetectionJobId);
    int pageNumber = geoJsonConversionTask.getPage() - 1;
    var paginatedDetectedTiles =
        computeDetectedTile(zoneDetectionType, zoneDetectionJobId, pageNumber);

    var zoneName = zoneDetectionJob.getZoneName();
    var fileKey =
        GEO_JSON_BUCKET_FOLDER
            + zoneDetectionJobId
            + "/"
            + zoneName
            + "-part"
            + pageNumber
            + GEO_JSON_EXTENSION;
    var geoJson = geoJsonConverter.convert(paginatedDetectedTiles);
    var geoJsonAsByte = writer.writeAsByte(geoJson);
    var geoJsonAsFile =
        writer.write(
            geoJsonAsByte,
            createTempDirectory(),
            zoneName + "-part" + pageNumber + GEO_JSON_EXTENSION);

    bucketComponent.upload(geoJsonAsFile, fileKey);

    geoJsonConversionTaskRepository.save(
        geoJsonConversionTask.toBuilder().fileKey(fileKey).build());
  }

  private List<DetectedTile> computeDetectedTile(
      ZoneDetectionJob.DetectionType zoneDetectionType, String zoneDetectionJobId, int pageNumber) {
    switch (zoneDetectionType) {
      case MACHINE -> {
        return humanDetectedTileRepository
            .findAllByJobId(zoneDetectionJobId, PageRequest.of(pageNumber, MAX_SIZE))
            .stream()
            .map(
                detectedTile ->
                    DetectedTile.builder()
                        .tile(detectedTile.getTile())
                        .detectedObjects(detectedTile.getDetectedObjects())
                        .build())
            .toList();
      }
      case HUMAN -> {
        return machineDetectedTileRepository
            .findAllByZdjJobId(zoneDetectionJobId, PageRequest.of(pageNumber, MAX_SIZE))
            .stream()
            .map(
                detectedTile ->
                    DetectedTile.builder()
                        .tile(detectedTile.getTile())
                        .detectedObjects(detectedTile.getDetectedObjects())
                        .build())
            .toList();
      }
      default ->
          throw new IllegalArgumentException("Unknown zoneDetectionType " + zoneDetectionType);
    }
  }
}
