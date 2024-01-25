package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionTaskCreated;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.ZoneDetectionTask;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ZoneDetectionJobService
    extends AbstractZoneJobService<
        ZoneDetectionTask, ZoneDetectionJob, ZoneDetectionJobRepository> {

  public ZoneDetectionJobService(
      EventProducer eventProducer, ZoneDetectionJobRepository repository) {
    super(eventProducer, repository);
  }

  public List<ZoneDetectionJob> process(String jobId) {
    var optionalZDJ = findById(jobId);
    var jobStatus =
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(optionalZDJ.getId())
            .jobType(DETECTION)
            .progression(Status.ProgressionStatus.PROCESSING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(now())
            .build();
    var toProcess = updateStatus(optionalZDJ, jobStatus);
    toProcess
        .getTasks()
        .forEach(task -> getEventProducer().accept(List.of(new ZoneDetectionTaskCreated(task))));
    return List.of(optionalZDJ);
  }

  public ZoneDetectionJob refreshStatus(String jobId) {
    var oldJob = findById(jobId);
    Status oldStatus = oldJob.getStatus();
    Status newStatus =
        Status.reduce(
            oldJob.getTasks().stream()
                .map(ZoneDetectionTask::getStatus)
                .map(status -> (Status) status)
                .toList());
    if (oldStatus.equals(newStatus)) {
      return oldJob;
    }

    var jobStatus = JobStatus.from(oldJob.getId(), newStatus, DETECTION);
    var refreshed = updateStatus(oldJob, jobStatus);

    getEventProducer()
        .accept(
            List.of(
                ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(refreshed).build()));
    return refreshed;
  }
}
