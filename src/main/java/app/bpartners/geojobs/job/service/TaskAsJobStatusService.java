package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import lombok.AllArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
public class TaskAsJobStatusService<T_CHILD extends Task, T_PARENT extends Task> {
  protected final TaskStatusRepository taskStatusRepository;
  protected final TaskAsJobService<T_CHILD, T_PARENT> taskAsJobService;

  @Transactional
  public T_CHILD process(T_CHILD task) {
    return update(task, PROCESSING, UNKNOWN, null);
  }

  @Transactional
  public T_CHILD succeed(T_CHILD task) {
    return update(task, FINISHED, SUCCEEDED, null);
  }

  @Transactional
  public T_CHILD fail(T_CHILD task) {
    return update(task, FINISHED, FAILED, null);
  }

  private T_CHILD update(
      T_CHILD childTask, ProgressionStatus progression, HealthStatus health, String message) {
    var parentTaskId = childTask.getParentTaskId();
    var oldParentTask = taskAsJobService.findById(parentTaskId);

    var taskStatus =
        TaskStatus.builder()
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(childTask.getId())
            .message(message)
            .build();
    childTask.hasNewStatus(taskStatus);
    taskStatusRepository.save(taskStatus);
    taskAsJobService.recomputeStatus(oldParentTask);

    return childTask;
  }
}
