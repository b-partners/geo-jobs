package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import org.springframework.transaction.annotation.Transactional;

public class TaskToTaskStatusService<T_CHILD extends Task, T_PARENT extends Task, J extends Job> {
  protected final TaskStatusService<T_PARENT, J> parentTaskStatusService;
  protected final TaskStatusRepository taskStatusRepository;
  protected final TaskToTaskService<T_CHILD, T_PARENT> taskToTaskService;

  public TaskToTaskStatusService(
      TaskStatusService<T_PARENT, J> parentTaskStatusService,
      TaskStatusRepository taskStatusRepository,
      TaskToTaskService<T_CHILD, T_PARENT> taskToTaskService) {
    this.parentTaskStatusService = parentTaskStatusService;
    this.taskStatusRepository = taskStatusRepository;
    this.taskToTaskService = taskToTaskService;
  }

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
    var oldParentTask = taskToTaskService.findById(parentTaskId);

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
    T_PARENT taskParent = taskToTaskService.recomputeStatus(oldParentTask);
    if (taskParent.getStatus().getProgression().equals(FINISHED)) {
      if (taskParent.getStatus().getHealth().equals(SUCCEEDED)) {
        parentTaskStatusService.succeed(taskParent);
      } else {
        parentTaskStatusService.fail(taskParent);
      }
    }
    return childTask;
  }
}
