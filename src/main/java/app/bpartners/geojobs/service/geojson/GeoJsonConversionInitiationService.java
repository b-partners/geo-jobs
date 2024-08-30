package app.bpartners.geojobs.service.geojson;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.time.Duration;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GeoJsonConversionInitiationService {
  private static final Duration PRE_SIGNED_URL_DURATION = Duration.ofHours(1L);
  private final GeoJsonConversionTaskService service;
  private final StatusMapper<TaskStatus> taskStatusMapper;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final BucketComponent bucketComponent;
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
    return processConversionTask(linkedJob.getZoneName(), jobId);
  }

  private String generatePresignedUrl(GeoJsonConversionTask task) {
    if (SUCCEEDED.equals(task.getStatus().getHealth())) {
      return bucketComponent.presign(task.getFileKey(), PRE_SIGNED_URL_DURATION).toString();
    }
    return null;
  }

  public GeoJsonsUrl processConversionTask(
      FullDetection fullDetection, String zoneName, String jobId) {
    var optionalTask = service.getByJobId(jobId);
    if (optionalTask.isPresent()) {
      var persisted = optionalTask.get();
      if (fullDetection.getGeojsonS3FileKey() != null
          && !FAILED.equals(persisted.getStatus().getHealth())) {
        var url = generatePresignedUrl(persisted);
        return new GeoJsonsUrl().url(url).status(taskStatusMapper.toRest(persisted.getStatus()));
      }
      service.delete(persisted);
    }
    return getGeoJsonsUrl(zoneName, jobId);
  }

  public GeoJsonsUrl processConversionTask(String zoneName, String jobId) {
    var optionalTask = service.getByJobId(jobId);
    if (optionalTask.isPresent()) {
      var persisted = optionalTask.get();
      if (!FAILED.equals(persisted.getStatus().getHealth())) {
        var url = generatePresignedUrl(persisted);
        return new GeoJsonsUrl().url(url).status(taskStatusMapper.toRest(persisted.getStatus()));
      }
      service.delete(persisted);
    }
    return getGeoJsonsUrl(zoneName, jobId);
  }

  private GeoJsonsUrl getGeoJsonsUrl(String zoneName, String jobId) {
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .fileKey(null)
            .submissionInstant(now())
            .statusHistory(List.of())
            .build();

    var saved = service.save(geoJsonConversionTask);
    eventProducer.accept(List.of(new GeoJsonConversionInitiated(jobId, saved.getId(), zoneName)));
    return new GeoJsonsUrl().url(null).status(taskStatusMapper.toRest(saved.getStatus()));
  }
}
