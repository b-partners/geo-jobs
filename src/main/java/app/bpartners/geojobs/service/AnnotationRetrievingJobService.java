package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.AnnotationRetrievingJobRepository;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingJob;
import app.bpartners.geojobs.repository.model.AnnotationRetrievingTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class AnnotationRetrievingJobService
    extends JobService<AnnotationRetrievingTask, AnnotationRetrievingJob> {
  private final AnnotationRetrievingJobRepository annotationRetrievingJobRepository;
  private final EventProducer eventProducer;

  protected AnnotationRetrievingJobService(
      JpaRepository<AnnotationRetrievingJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<AnnotationRetrievingTask> taskRepository,
      EventProducer eventProducer,
      AnnotationRetrievingJobRepository annotationRetrievingJobRepository) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        AnnotationRetrievingJob.class);
    this.annotationRetrievingJobRepository = annotationRetrievingJobRepository;
    this.eventProducer = eventProducer;
  }

  public AnnotationRetrievingJob fireTasks(String jobId) {
    var job = findById(jobId);
    getTasks(job)
        .forEach(
            task ->
                eventProducer.accept(
                    List.of(
                        AnnotationRetrievingTaskCreated.builder()
                            .annotationRetrievingTask(task)
                            .build())));

    return job;
  }

  public List<AnnotationRetrievingJob> findAllByDetectionJobId(String id) {
    return annotationRetrievingJobRepository.findAllByDetectionJobId(id);
  }

  public AnnotationRetrievingJob getByAnnotationJobId(String id) {
    return annotationRetrievingJobRepository
        .findByAnnotationJobId(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "No Annotation retrieving job found for annotation job id=" + id));
  }

  public List<AnnotationRetrievingJob> saveAll(List<AnnotationRetrievingJob> toSave) {
    return annotationRetrievingJobRepository.saveAll(toSave);
  }

  @Override
  protected void onStatusChanged(AnnotationRetrievingJob oldJob, AnnotationRetrievingJob newJob) {
    eventProducer.accept(
        List.of(
            AnnotationRetrievingJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
