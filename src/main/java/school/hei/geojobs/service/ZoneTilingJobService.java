package school.hei.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import school.hei.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import school.hei.geojobs.endpoint.event.gen.ZoneTilingTaskCreated;
import school.hei.geojobs.model.exception.NotFoundException;
import school.hei.geojobs.repository.ZoneTilingJobRepository;
import school.hei.geojobs.repository.model.Status;
import school.hei.geojobs.repository.model.TilingJobStatus;
import school.hei.geojobs.repository.model.ZoneTilingJob;
import school.hei.geojobs.repository.model.ZoneTilingTask;
import school.hei.geojobs.repository.model.geo.Parcel;

@Service
public class ZoneTilingJobService
    extends AbstractZoneJobService<
        TilingJobStatus, ZoneTilingTask, ZoneTilingJob, ZoneTilingJobRepository> {

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
        .allMatch(status -> PENDING.equals(status.getProgression()));
  }

  public ZoneTilingJob process(ZoneTilingJob job) {
    var status =
        TilingJobStatus.builder()
            .id(randomUUID().toString())
            .jobId(job.getId())
            .progression(PROCESSING)
            .health(UNKNOWN)
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
    var jobStatus = TilingJobStatus.from(oldJob.getId(), newStatus);
    var refreshed = updateStatus(oldJob, jobStatus);

    getEventProducer()
        .accept(
            List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(refreshed).build()));
    return refreshed;
  }
}
