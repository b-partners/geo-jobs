package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusChanged;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class AnnotationRetrievingJobStatusChangedService
    implements Consumer<AnnotationRetrievingJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final AnnotationRetrievingJobService retrievingJobService;
  private final JobStatusRepository jobStatusRepository;
  private final GeoJsonConversionInitiationService geoJsonConversionInitiationService;

  @Override
  public void accept(AnnotationRetrievingJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    OnFinishedHandler onFinishedHandler =
        new OnFinishedHandler(
            zoneDetectionJobService,
            newJob,
            retrievingJobService,
            jobStatusRepository,
            geoJsonConversionInitiationService);
    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(
      ZoneDetectionJobService zoneDetectionJobService,
      AnnotationRetrievingJob newJob,
      AnnotationRetrievingJobService retrievingJobService,
      JobStatusRepository jobStatusRepository,
      GeoJsonConversionInitiationService geoJsonConversionInitiationService)
      implements StatusHandler {

    @Override
    public String performAction() {
      // TODO: add HumanDetectionTask in ZoneDetectionJob.type=HUMAN linked to
      // AnnotationRetrievingJob
      String detectionJobId = newJob.getDetectionJobId();
      var oldHumanZDJ = zoneDetectionJobService.findById(detectionJobId);
      var oldHumanZDJStatus = oldHumanZDJ.getStatus();

      var newZDJ = (ZoneDetectionJob) oldHumanZDJ.semanticClone();
      var linkedRetrievingJobs = retrievingJobService.findAllByDetectionJobId(detectionJobId);
      boolean linkedJobsAreSucceeded = linkedRetrievingJobs.stream().allMatch(Job::isSucceeded);
      if (linkedJobsAreSucceeded) {
        newZDJ.hasNewStatus(getJobStatus(newZDJ, FINISHED, SUCCEEDED));
      }

      var newStatus = newZDJ.getStatus();
      if (!oldHumanZDJStatus.getProgression().equals(newStatus.getProgression())
          || !oldHumanZDJStatus.getHealth().equals(newStatus.getHealth())) {
        jobStatusRepository.save(newStatus);
      }
      if (!oldHumanZDJ.isFinished() && newZDJ.isFinished()) {
        geoJsonConversionInitiationService.processConversionTask(
            newZDJ.getZoneName(), newZDJ.getId());
        return "AnnotationRetrievedJob (id"
            + newJob.getId()
            + ") finished with status "
            + newJob.getStatus()
            + " and processing ZDJ(id="
            + newZDJ.getId()
            + ") geo json conversion";
      }
      return "AnnotationRetrievedJob (id"
          + newJob.getId()
          + ") finished with status "
          + newJob.getStatus();
    }
  }

  private static JobStatus getJobStatus(
      ZoneDetectionJob newZDJ,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return JobStatus.builder()
        .id(randomUUID().toString())
        .jobId(newZDJ.getId())
        .progression(progressionStatus)
        .health(healthStatus)
        .jobType(DETECTION)
        .creationDatetime(now())
        .build();
  }
}
