package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
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
public abstract class JobService<T extends Task, J extends Job> {
  protected final JpaRepository<J, String> repository;
  protected final JobStatusRepository jobStatusRepository;
  protected final TaskRepository<T> taskRepository;
  protected final EventProducer eventProducer;
  private final Class<J> jobClazz;

  @Setter @PersistenceContext EntityManager em;

  protected JobService(
      JpaRepository<J, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<T> taskRepository,
      EventProducer eventProducer,
      Class<J> jobClazz) {
    this.repository = repository;
    this.jobStatusRepository = jobStatusRepository;
    this.taskRepository = taskRepository;
    this.eventProducer = eventProducer;
    this.jobClazz = jobClazz;
  }

  public List<J> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("job.id=" + id));
  }

  public J recomputeStatus(J oldJob) {
    var jobId = oldJob.getId();
    var oldStatus = oldJob.getStatus();

    em.detach(oldJob); // else following getXxx will still retrieve latest version from db
    var newJob = (J) oldJob.semanticClone();

    var tasks = taskRepository.findAllByJobId(jobId);
    tasks.forEach(em::detach); // else maintaining task <--> taskStatus will result in SQL updates
    var newTaskStatuses = tasks.stream().map(Task::getStatus).collect(toList());
    newJob.hasNewStatus(jobStatusFromTaskStatuses(jobId, oldStatus, newTaskStatuses));

    var newStatus = newJob.getStatus();
    if (!oldStatus.getProgression().equals(newStatus.getProgression())
        || !oldStatus.getHealth().equals(newStatus.getHealth())) {
      onStatusChanged(oldJob, newJob);
      jobStatusRepository.save(newStatus);
    }
    return newJob;
  }

  private JobStatus jobStatusFromTaskStatuses(
      String jobId, JobStatus oldStatus, List<TaskStatus> taskStatuses) {
    return JobStatus.from(
        jobId,
        Status.builder()
            .progression(progressionFromTaskStatus(oldStatus, taskStatuses))
            .health(healthFromTaskStatuses(oldStatus, taskStatuses))
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

  private Status.HealthStatus healthFromTaskStatuses(
      JobStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newHealths = newTaskStatuses.stream().map(Status::getHealth).toList();
    return newHealths.stream().anyMatch(FAILED::equals)
        ? FAILED
        : newHealths.stream().anyMatch(UNKNOWN::equals)
            ? UNKNOWN
            : newHealths.stream().allMatch(SUCCEEDED::equals) ? SUCCEEDED : oldStatus.getHealth();
  }

  private Status.ProgressionStatus progressionFromTaskStatus(
      JobStatus oldStatus, List<TaskStatus> newTaskStatuses) {
    var newProgressions = newTaskStatuses.stream().map(Status::getProgression).toList();
    return newProgressions.stream().anyMatch(PROCESSING::equals)
        ? PROCESSING
        : newProgressions.stream().allMatch(FINISHED::equals)
            ? FINISHED
            : oldStatus.getProgression();
  }

  protected void onStatusChanged(J oldJob, J newJob) {}

  public J create(J job, List<T> tasks) {
    if (!job.isPending()) {
      throw new IllegalArgumentException(
          "Only PENDING job can be created. " + "You sure all tasks are PENDING?");
    }

    var saved = repository.save(job);
    taskRepository.saveAll(tasks);
    return saved;
  }

  protected List<T> getTasks(J job) {
    return taskRepository.findAllByJobId(job.getId());
  }
}
