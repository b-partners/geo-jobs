package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingTaskCreated;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.model.ZoneTilingTask;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ZoneTilingJobService
    extends AbstractZoneJobService<ZoneTilingTask, ZoneTilingJob, ZoneTilingJobRepository> {

  public ZoneTilingJobService(EventProducer eventProducer, ZoneTilingJobRepository repository) {
    super(eventProducer, repository);
  }

  public ZoneTilingJob create(ZoneTilingJob job) {
    if (!areAllTasksPending(job)) {
      throw new IllegalArgumentException("Tasks on job creation must all have status PENDING");
    }

    var saved = getRepository().save(job);
    getEventProducer().accept(List.of(new ZoneTilingJobCreated(job)));
    return saved;
  }

  public List<Parcel> getAJobParcel(String jobId) {
    Optional<ZoneTilingJob> zoneTilingJob = getRepository().findById(jobId);

    if (zoneTilingJob.isPresent()) {
      return zoneTilingJob.get().getTasks().stream().map(ZoneTilingTask::getParcel).toList();
    }

    throw new NotFoundException("The job is not found");
  }

  private static boolean areAllTasksPending(ZoneTilingJob job) {
    return job.getTasks().stream()
        .map(ZoneTilingTask::getStatus)
        .allMatch(status -> Status.ProgressionStatus.PENDING.equals(status.getProgression()));
  }

  public ZoneTilingJob process(ZoneTilingJob job) {
    var status =
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(job.getId())
            .progression(Status.ProgressionStatus.PROCESSING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(now())
            .build();
    var processed = updateStatus(job, status);
    job.getTasks()
        .forEach(task -> getEventProducer().accept(List.of(new ZoneTilingTaskCreated(task))));
    return processed;
  }

  public ZoneTilingJob refreshStatus(String jobId) {
    var oldJob = findById(jobId);
    Status oldStatus = oldJob.getStatus();
    Status newStatus =
        Status.reduce(
            oldJob.getTasks().stream()
                .map(ZoneTilingTask::getStatus)
                .map(status -> (Status) status)
                .toList());
    if (oldStatus.equals(newStatus)) {
      return oldJob;
    }
    var jobStatus = JobStatus.from(oldJob.getId(), newStatus, TILING);
    var refreshed = updateStatus(oldJob, jobStatus);

    getEventProducer()
        .accept(
            List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(refreshed).build()));
    return refreshed;
  }
}
