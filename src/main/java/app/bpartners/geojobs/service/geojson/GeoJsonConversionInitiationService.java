package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConversionInitiationService {
  private final GeoJsonConversionTaskService service;
  private final StatusMapper<TaskStatus> taskStatusMapper;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final EventProducer<GeoJsonConversionInitiated> eventProducer;

  public GeoJsonsUrl initiateGeoJsonConversion(String jobId) {
    var linkedJob = zoneDetectionJobService.getHumanZdjFromZdjId(jobId);
    var jobStatus = linkedJob.getStatus();
    if (!FINISHED.equals(jobStatus.getProgression()) && !SUCCEEDED.equals(jobStatus.getHealth())) {
      return new GeoJsonsUrl()
          .url(
              "Unable to generate geoJsons Url to unfinished succeeded job. Actual status is "
                  + jobStatus)
          .status(null);
    }
    return processConversionTask(jobId);
  }

  private GeoJsonsUrl processConversionTask(String jobId) {
    var persisted = service.getByJobId(jobId);
    if (persisted != null) {
      return new GeoJsonsUrl()
          .url(persisted.getGeoJsonUrl())
          .status(taskStatusMapper.toRest(persisted.getStatus()));
    }
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .geoJsonUrl(null) // Null until task is finished
            .submissionInstant(now())
            .statusHistory(List.of())
            .build();

    var saved = service.save(geoJsonConversionTask);
    eventProducer.accept(List.of(new GeoJsonConversionInitiated(jobId, saved.getId())));
    return new GeoJsonsUrl()
        .url(saved.getGeoJsonUrl())
        .status(taskStatusMapper.toRest(saved.getStatus()));
  }
}
