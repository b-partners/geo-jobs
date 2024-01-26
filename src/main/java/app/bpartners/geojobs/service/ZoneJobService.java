package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.ZoneJob;
import app.bpartners.geojobs.repository.model.ZoneTask;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

// TODO: Consider refactoring for better abstraction - warrants discussion
@AllArgsConstructor
@Data
@Service
public class ZoneJobService<T extends ZoneTask, J extends ZoneJob<T>> {
  private final EventProducer eventProducer;

  public List<J> findAll(
      PageFromOne page, BoundedPageSize pageSize, JpaRepository<J, String> repository) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id, JpaRepository<J, String> repository) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ZoneJob.Id " + id + " not found"));
  }

  public J updateStatus(J job, JobStatus status, JpaRepository<J, String> repository) {
    job.addStatus(status);
    return repository.save(job);
  }

  public J create(J job, JpaRepository<J, String> repository) {
    if (!areAllTasksPending(job)) {
      throw new IllegalArgumentException("Tasks on job creation must all have status PENDING");
    }

    return repository.save(job);
  }

  public J process(J job, JobStatus.JobType jobType, JpaRepository<J, String> repository) {
    var jobStatus =
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(job.getId())
            .jobType(jobType)
            .progression(Status.ProgressionStatus.PROCESSING)
            .health(Status.HealthStatus.UNKNOWN)
            .creationDatetime(now())
            .build();
    return updateStatus(job, jobStatus, repository);
  }

  private boolean areAllTasksPending(J job) {
    return job.getTasks().stream()
        .map(T::getStatus)
        .allMatch(status -> Status.ProgressionStatus.PENDING.equals(status.getProgression()));
  }
}
