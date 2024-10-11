package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
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
  private final EventProducer eventProducer;

  public AnnotationDeliveryJobService(
      JpaRepository<AnnotationDeliveryJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<AnnotationDeliveryTask> taskRepository,
      EventProducer eventProducer) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        AnnotationDeliveryJob.class);
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
    eventProducer.accept(List.of(new AnnotationDeliveryJobStatusRecomputingSubmitted(jobId)));
    return job;
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
