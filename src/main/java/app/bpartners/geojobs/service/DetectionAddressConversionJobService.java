package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskCreated;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.TaskStatisticRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetectionAddressConversionJobService
    extends JobService<DetectionAddressConversionTask, DetectionAddressConversionJob> {

  protected DetectionAddressConversionJobService(
      JpaRepository<DetectionAddressConversionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskStatisticRepository taskStatisticRepository,
      TaskRepository<DetectionAddressConversionTask> taskRepository,
      EventProducer eventProducer) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        DetectionAddressConversionJob.class);
  }

  @Transactional
  public DetectionAddressConversionJob fireTasks(String jobId) {
    var job = findById(jobId);
    getTasks(job)
        .forEach(
            task -> {
              eventProducer.accept(
                  List.of(DetectionAddressConversionTaskCreated.builder().task(task).build()));
            });

    eventProducer.accept(
        List.of(new DetectionAddressConversionJobStatusRecomputingSubmitted(jobId)));

    return job;
  }

  @Override
  protected void onStatusChanged(
      DetectionAddressConversionJob oldJob, DetectionAddressConversionJob newJob) {
    eventProducer.accept(
        List.of(
            DetectionAddressConversionJobStatusChanged.builder()
                .oldJob(oldJob)
                .newJob(newJob)
                .build()));
  }
}
