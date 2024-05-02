package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.*;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@Slf4j
public abstract class TaskToTaskService<T_CHILD extends Task, T_PARENT extends Task> {
  protected final JpaRepository<T_PARENT, String> parentRepository;
  protected final TaskStatusRepository taskStatusRepository;
  protected final TaskRepository<T_CHILD> childTaskRepository;
  protected final EventProducer eventProducer;
  private final Class<T_PARENT> jobClazz;

  @Setter @PersistenceContext EntityManager em;

  protected TaskToTaskService(
      JpaRepository<T_PARENT, String> parentRepository,
      TaskStatusRepository taskStatusRepository,
      TaskRepository<T_CHILD> childTaskRepository,
      EventProducer eventProducer,
      Class<T_PARENT> jobClazz) {
    this.parentRepository = parentRepository;
    this.taskStatusRepository = taskStatusRepository;
    this.childTaskRepository = childTaskRepository;
    this.eventProducer = eventProducer;
    this.jobClazz = jobClazz;
  }

  public List<T_PARENT> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return parentRepository.findAll(pageable).toList();
  }

  public T_PARENT findById(String id) {
    return parentRepository.findById(id).orElseThrow(() -> new NotFoundException("job.id=" + id));
  }

  public T_PARENT recomputeStatus(T_PARENT oldTask) {
    var parentTaskId = oldTask.getId();
    var oldStatus = oldTask.getStatus();

    em.detach(oldTask); // else following getXxx will still retrieve latest version from db
    var newParentTask = (T_PARENT) oldTask.semanticClone();

    var childTasks = childTaskRepository.findAllByParentTaskId(parentTaskId);
    childTasks.forEach(
        em::detach); // else maintaining task <--> taskStatus will result in SQL updates
    var allChildTaskStatuses = childTasks.stream().map(Task::getStatus).collect(toList());
    newParentTask.hasNewStatus(
        parentStatusFromChildStatuses(parentTaskId, oldStatus, allChildTaskStatuses));

    var newStatus = newParentTask.getStatus();
    if (!oldStatus.getProgression().equals(newStatus.getProgression())
        || !oldStatus.getHealth().equals(newStatus.getHealth())) {
      onStatusChanged(oldTask, newParentTask);
      taskStatusRepository.save(newStatus);
    }
    return newParentTask;
  }

  private TaskStatus parentStatusFromChildStatuses(
      String jobId, TaskStatus oldStatus, List<TaskStatus> taskStatuses) {
    return TaskStatus.from(
        jobId,
        Status.builder()
            .progression(progressionFromTaskStatus(oldStatus, taskStatuses))
            .health(parentHealthFromChildStatuses(oldStatus, taskStatuses))
            .creationDatetime(
                latestInstantFromTaskStatuses(taskStatuses, oldStatus.getCreationDatetime()))
            .build(),
        oldStatus.getJobType());
  }

  private Instant latestInstantFromTaskStatuses(
      List<TaskStatus> taskStatuses, Instant defaultInstant) {
    var sortedInstants =
        taskStatuses.stream()
            .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
            .toList();
    return sortedInstants.isEmpty() ? defaultInstant : sortedInstants.get(0).getCreationDatetime();
  }

  private Status.HealthStatus parentHealthFromChildStatuses(
      TaskStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newHealths = newTaskStatuses.stream().map(Status::getHealth).toList();
    return newHealths.stream().anyMatch(FAILED::equals)
        ? FAILED
        : newHealths.stream().anyMatch(UNKNOWN::equals)
            ? UNKNOWN
            : newHealths.stream().allMatch(SUCCEEDED::equals) ? SUCCEEDED : oldStatus.getHealth();
  }

  private Status.ProgressionStatus progressionFromTaskStatus(
      TaskStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newProgressions = newTaskStatuses.stream().map(Status::getProgression).toList();
    return newProgressions.stream().anyMatch(PROCESSING::equals)
        ? PROCESSING
        : newProgressions.stream().allMatch(FINISHED::equals)
            ? FINISHED
            : oldStatus.getProgression();
  }

  protected void onStatusChanged(T_PARENT oldJob, T_PARENT newJob) {}

  public T_PARENT create(T_PARENT job, List<T_CHILD> tasks) {
    if (!job.isPending()) {
      throw new IllegalArgumentException(
          "Only PENDING job can be created. " + "You sure all tasks are PENDING?");
    }

    var saved = parentRepository.save(job);
    childTaskRepository.saveAll(tasks);
    return saved;
  }

  protected List<T_CHILD> getTasks(T_PARENT job) {
    return childTaskRepository.findAllByJobId(job.getId());
  }
}
