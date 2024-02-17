package app.bpartners.geojobs.job.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class JobService<T extends Task, J extends Job> {
  protected final JpaRepository<J, String> repository;
  protected final TaskRepository<T> taskRepository;
  protected final EventProducer eventProducer;
  private final Class<J> jobClazz;

  protected JobService(
      JpaRepository<J, String> repository,
      TaskRepository<T> taskRepository,
      EventProducer eventProducer,
      Class<J> jobClazz) {
    this.repository = repository;
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
    var newJob = repository.findById(oldJob.getId()).get();
    newJob.hasNewStatus(
        jobStatusFromTasks(jobId, oldStatus, taskRepository.findAllByJobId(oldJob.getId())));
    newJob = repository.save(newJob);

    var newStatus = newJob.getStatus();
    if (!oldStatus.getProgression().equals(newStatus.getProgression())
        || !oldStatus.getHealth().equals(newStatus.getHealth())) {
      onStatusChanged(oldJob, newJob);
    }
    return newJob;
  }

  private JobStatus jobStatusFromTasks(String jobId, JobStatus oldStatus, List<T> tasks) {
    return JobStatus.from(
        jobId,
        Status.builder()
            .progression(progressionFromTasks(oldStatus, tasks))
            .health(healthFromTasks(oldStatus, tasks))
            .creationDatetime(latestInstantFromTasks(tasks, oldStatus.getCreationDatetime()))
            .build(),
        oldStatus.getJobType());
  }

  private Instant latestInstantFromTasks(List<T> tasks, Instant defaultInstant) {
    var sortedInstants =
        tasks.stream()
            .map(Task::getStatus)
            .sorted(comparing(Status::getCreationDatetime, naturalOrder()).reversed())
            .toList();
    return sortedInstants.isEmpty() ? defaultInstant : sortedInstants.get(0).getCreationDatetime();
  }

  private Status.HealthStatus healthFromTasks(JobStatus oldStatus, List<T> newTasks) {
    var newHealths = newTasks.stream().map(Task::getStatus).map(Status::getHealth).toList();
    return newHealths.stream().anyMatch(FAILED::equals)
        ? FAILED
        : newHealths.stream().anyMatch(UNKNOWN::equals)
            ? UNKNOWN
            : newHealths.stream().allMatch(SUCCEEDED::equals) ? SUCCEEDED : oldStatus.getHealth();
  }

  private Status.ProgressionStatus progressionFromTasks(JobStatus oldStatus, List<T> newTasks) {
    var newProgressions =
        newTasks.stream().map(Task::getStatus).map(Status::getProgression).toList();
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

  @Setter @PersistenceContext EntityManager em;

  @SneakyThrows
  public J pwFindById(String id) {
    return em.find(jobClazz, id, PESSIMISTIC_WRITE);
  }
}
