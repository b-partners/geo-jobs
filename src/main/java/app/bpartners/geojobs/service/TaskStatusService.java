package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.Status.HealthStatus;
import app.bpartners.geojobs.repository.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.model.Task;
import app.bpartners.geojobs.repository.model.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class TaskStatusService<T extends Task, J extends Job<T>> {

  protected final JpaRepository<T, String> repository;
  protected final JobService<T, J> jobService;

  public TaskStatusService(JpaRepository<T, String> repository, JobService<T, J> jobService) {
    this.repository = repository;
    this.jobService = jobService;
  }

  @Transactional
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
    var oldJob = jobService.findById(jobIb);
    em.detach(oldJob); // else future getXxx will still retrieve latest version from db

    task.addStatus(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build());
    var updatedTask = repository.save(task);
    jobService.refreshStatus(oldJob);

    return updatedTask;
  }
}
