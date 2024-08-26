package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusChanged;
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
import java.time.Instant;
import java.util.List;
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
      var newHumanZDJ = (ZoneDetectionJob) oldHumanZDJ.semanticClone();
      List<AnnotationRetrievingJob> linkedRetrievingJobs =
          retrievingJobService.findAllByDetectionJobId(detectionJobId);

      var newRetrievingJobsStatuses =
          linkedRetrievingJobs.stream().map(AnnotationRetrievingJob::getStatus).collect(toList());
      newHumanZDJ.hasNewStatus(
          jobStatusFromRetrievingJobStatuses(
              oldHumanZDJ.getId(), oldHumanZDJStatus, newRetrievingJobsStatuses));

      var newStatus = newHumanZDJ.getStatus();
      if (!oldHumanZDJStatus.getProgression().equals(newStatus.getProgression())
          || !oldHumanZDJStatus.getHealth().equals(newStatus.getHealth())) {
        jobStatusRepository.save(newStatus);
        if (newHumanZDJ.isFinished()) {
          geoJsonConversionInitiationService.processConversionTask(
              newHumanZDJ.getZoneName(), newHumanZDJ.getId());
        }
      }
      return "AnnotationRetrievedJob (id"
          + newJob.getId()
          + ") finished with status "
          + newJob.getStatus();
    }

    private JobStatus jobStatusFromRetrievingJobStatuses(
        String jobId, JobStatus oldStatus, List<JobStatus> linkedJobStatuses) {
      return JobStatus.from(
          jobId,
          Status.builder()
              .progression(progressionFromTaskStatus(oldStatus, linkedJobStatuses))
              .health(healthFromTaskStatuses(oldStatus, linkedJobStatuses))
              .creationDatetime(
                  latestInstantFromTaskStatuses(linkedJobStatuses, oldStatus.getCreationDatetime()))
              .build(),
          oldStatus.getJobType());
    }

    private Status.ProgressionStatus progressionFromTaskStatus(
        JobStatus oldStatus, List<JobStatus> newLinkedJobStatuses) {
      var newProgressions = newLinkedJobStatuses.stream().map(Status::getProgression).toList();
      return newProgressions.stream().anyMatch(PROCESSING::equals)
          ? PROCESSING
          : newProgressions.stream().allMatch(FINISHED::equals)
              ? FINISHED
              : oldStatus.getProgression();
    }

    private Status.HealthStatus healthFromTaskStatuses(
        JobStatus oldStatus, List<JobStatus> newLinkedJobStatuses) {
      var newHealths = newLinkedJobStatuses.stream().map(Status::getHealth).toList();
      return newHealths.stream().anyMatch(FAILED::equals)
          ? FAILED
          : newHealths.stream().anyMatch(UNKNOWN::equals)
              ? UNKNOWN
              : newHealths.stream().allMatch(SUCCEEDED::equals) ? SUCCEEDED : oldStatus.getHealth();
    }

    private Instant latestInstantFromTaskStatuses(
        List<JobStatus> linkedJobStatuses, Instant defaultInstant) {
      var sortedInstants =
          linkedJobStatuses.stream()
              .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
              .toList();
      return sortedInstants.isEmpty()
          ? defaultInstant
          : sortedInstants.getFirst().getCreationDatetime();
    }
  }
}
