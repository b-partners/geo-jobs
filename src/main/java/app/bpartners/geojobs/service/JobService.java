package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.TaskRepository;
import app.bpartners.geojobs.repository.model.Job;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Task;
import app.bpartners.geojobs.repository.model.TaskStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@AllArgsConstructor
public abstract class JobService<T extends Task, J extends Job> {
  protected final JpaRepository<J, String> repository;
  protected final TaskRepository<T> taskRepository;
  protected final EventProducer eventProducer;

  public List<J> findAll(PageFromOne page, BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return repository.findAll(pageable).toList();
  }

  public J findById(String id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("job.id=" + id));
  }

  public J hasNewTaskStatus(J oldJob, TaskStatus taskStatus) {
    var newJob = repository.findById(oldJob.getId()).orElseThrow();
    newJob.hasNewStatus(reduce(oldJob.getStatus(), taskStatus));
    newJob = repository.save(newJob);

    var oldStatus = oldJob.getStatus();
    var newStatus = newJob.getStatus();
    if (!oldStatus.getProgression().equals(newStatus.getProgression())
        || !oldStatus.getHealth().equals(newStatus.getHealth())) {
      onStatusChanged(oldJob, newJob);
    }
    return newJob;
  }

  private JobStatus reduce(JobStatus jobStatus, TaskStatus taskStatus) {
    return jobStatus.toBuilder()
        .progression(taskStatus.getProgression())
        .health(taskStatus.getHealth())
        .creationDatetime(taskStatus.getCreationDatetime())
        .build();
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
