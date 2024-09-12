package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationDeliveryJobRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnotationDeliveryJobService
    extends JobService<AnnotationDeliveryTask, AnnotationDeliveryJob> {
  private final AnnotationDeliveryJobRepository AnnotationDeliveryJobRepository;
  private final EventProducer eventProducer;

  protected AnnotationDeliveryJobService(
      JpaRepository<AnnotationDeliveryJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<AnnotationDeliveryTask> taskRepository,
      EventProducer eventProducer,
      AnnotationDeliveryJobRepository AnnotationDeliveryJobRepository) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        AnnotationDeliveryJob.class);
    this.AnnotationDeliveryJobRepository = AnnotationDeliveryJobRepository;
    this.eventProducer = eventProducer;
  }

  @Transactional
  public AnnotationDeliveryJob fireTasks(String jobId) {
    var job = findById(jobId);
    List<AnnotationDeliveryTask> tasks = getTasks(job);
    tasks.forEach(
        task ->
            eventProducer.accept(
                List.of(AnnotationDeliveryTaskCreated.builder().deliveryTask(task).build())));

    return job;
  }

  public List<AnnotationDeliveryJob> findAllByDetectionJobId(String id) {
    return AnnotationDeliveryJobRepository.findAllByDetectionJobId(id);
  }

  public AnnotationDeliveryJob getByAnnotationJobId(String id) {
    return AnnotationDeliveryJobRepository.findByAnnotationJobId(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No Annotation delivery job found for annotation job id=" + id));
  }

  public List<AnnotationDeliveryJob> saveAll(List<AnnotationDeliveryJob> toSave) {
    return AnnotationDeliveryJobRepository.saveAll(toSave);
  }

  @Override
  @Transactional
  public AnnotationDeliveryJob create(
      AnnotationDeliveryJob job, List<AnnotationDeliveryTask> tasks) {
    AnnotationDeliveryJob newJob = super.create(job, tasks);
    eventProducer.accept(
        List.of(AnnotationDeliveryJobCreated.builder().deliveryJob(newJob).build()));
    return newJob;
  }

  @Override
  protected void onStatusChanged(AnnotationDeliveryJob oldJob, AnnotationDeliveryJob newJob) {
    eventProducer.accept(
        List.of(
            AnnotationDeliveryJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
