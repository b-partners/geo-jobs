package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ZDJStatusRecomputingSubmittedService
    implements Consumer<ZDJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneDetectionJob, ParcelDetectionTask, ZDJStatusRecomputingSubmitted>
      service;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final JobAnnotationService jobAnnotationService;

  public ZDJStatusRecomputingSubmittedService(
      ZoneDetectionJobService jobService,
      EventProducer eventProducer,
      TaskStatusService<ParcelDetectionTask> taskStatusService,
      TaskRepository<ParcelDetectionTask> taskRepository,
      ZoneDetectionJobService zoneDetectionJobService,
      JobAnnotationService jobAnnotationService) {
    this.zoneDetectionJobService = zoneDetectionJobService;
    this.jobAnnotationService = jobAnnotationService;
    this.service =
        new JobStatusRecomputingSubmittedService<>(
            eventProducer, jobService, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ZDJStatusRecomputingSubmitted event) {
    service.accept(event);
    ZoneDetectionJob zoneDetectionJob = zoneDetectionJobService.findById(event.getJobId());
    if (zoneDetectionJob.isFinished() && zoneDetectionJob.isSucceeded()) {
      jobAnnotationService.processAnnotationJob(event.getJobId(), Double.valueOf(0.8));
    }
  }
}
