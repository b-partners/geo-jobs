package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.model.page.BoundedPageSize.MAX_SIZE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GeoJsonConversionJobCreatedService implements Consumer<GeoJsonConversionJobCreated> {

  private final HumanDetectedTileRepository humanDetectedTileRepository;
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(GeoJsonConversionJobCreated event) {
    var geoJsonConversionJob = event.getGeoJsonConversionJob();
    var zoneDetectionJobId = geoJsonConversionJob.getZoneDetectionJobId();
    var zoneDetectionJobType = geoJsonConversionJob.getZoneDetectionJobType();
    var tilesCount = computeDetectedTilesCount(zoneDetectionJobType, zoneDetectionJobId);
    int pageSize = Math.max(1, (int) Math.ceil((double) tilesCount / MAX_SIZE));
    var conversionTasks = new ArrayList<GeoJsonConversionTask>();
    for (int pageValue = 0; pageValue < pageSize; pageValue++) {
      var geoJsonConversionTask = fromConversionJobAndPage(geoJsonConversionJob, pageValue);
      conversionTasks.add(geoJsonConversionTask);
    }
    var savedConversionTasks = geoJsonConversionTaskRepository.saveAll(conversionTasks);
    savedConversionTasks.forEach(
        conversionTask ->
            eventProducer.accept(
                List.of(
                    GeoJsonConversionTaskCreated.builder()
                        .geoJsonConversionTask(conversionTask)
                        .build())));

    eventProducer.accept(
        List.of(new GeoJsonConversionJobStatusRecomputingSubmitted(geoJsonConversionJob.getId())));
  }

  private GeoJsonConversionTask fromConversionJobAndPage(
      GeoJsonConversionJob geoJsonConversionJob, int pageValue) {
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(randomUUID().toString())
            .jobId(geoJsonConversionJob.getId())
            .page(pageValue + 1)
            .submissionInstant(now())
            .build();
    geoJsonConversionTask.hasNewStatus(
        Status.builder()
            .progression(PENDING)
            .health(UNKNOWN)
            .creationDatetime(now())
            .message(null)
            .build());
    return geoJsonConversionTask;
  }

  private Long computeDetectedTilesCount(
      ZoneDetectionJob.DetectionType zoneDetectionJobType, String zoneDetectionJobId) {
    switch (zoneDetectionJobType) {
      case HUMAN -> {
        return humanDetectedTileRepository.countByJobId(zoneDetectionJobId);
      }
      case MACHINE -> {
        return machineDetectedTileRepository.countByZdjJobId(zoneDetectionJobId);
      }
      default ->
          throw new IllegalArgumentException("Unknown zoneDetectionType " + zoneDetectionJobType);
    }
  }
}
