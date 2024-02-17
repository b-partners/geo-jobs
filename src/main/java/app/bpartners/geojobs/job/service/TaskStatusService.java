package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class TaskStatusService<T extends Task, J extends Job> {

  protected final JpaRepository<T, String> repository;
  protected final JobService<T, J> jobService;

  public TaskStatusService(JpaRepository<T, String> repository, JobService<T, J> jobService) {
    this.repository = repository;
    this.jobService = jobService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public T process(T task) {
    return update(task, PROCESSING, UNKNOWN);
  }

  @Transactional
  public T succeed(T task) {
    return update(task, FINISHED, SUCCEEDED);
  }

  @Transactional
  public T fail(T task) {
    return update(task, FINISHED, FAILED);
  }

  @Setter @PersistenceContext EntityManager em;

  private T update(T task, ProgressionStatus progression, HealthStatus health) {
    var taskId = task.getId();
    if (!repository.existsById(taskId)) {
      throw new NotFoundException("task.id=" + taskId);
    }
    var jobIb = task.getJobId();
    var oldJob = jobService.pwFindById(jobIb);
    em.detach(oldJob); // else future getXxx will still retrieve latest version from db

    TaskStatus taskStatus =
        TaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build();
    task.hasNewStatus(taskStatus);
    var updatedTask = repository.save(task);
    jobService.recomputeStatus(oldJob);

    return updatedTask;
  }
}
