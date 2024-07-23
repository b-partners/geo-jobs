package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.ZoneService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class ZTJStatusRecomputingSubmittedService
    implements Consumer<ZTJStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ZoneTilingJob, TilingTask, ZTJStatusRecomputingSubmitted>
      service;
  private final ZoneTilingJobRepository zoneTilingJobRepository;
  private final ZoneService zoneService;

  public ZTJStatusRecomputingSubmittedService(
      ZoneTilingJobService jobService,
      EventProducer eventProducer,
      TaskStatusService<TilingTask> taskStatusService,
      TaskRepository<TilingTask> taskRepository,
      ZoneTilingJobRepository zoneTilingJobRepository,
      ZoneService zoneService) {
    this.zoneTilingJobRepository = zoneTilingJobRepository;
    this.zoneService = zoneService;
    this.service =
        new JobStatusRecomputingSubmittedService<>(
            eventProducer, jobService, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ZTJStatusRecomputingSubmitted event) {
    Optional<ZoneTilingJob> zoneTilingJob = zoneTilingJobRepository.findById(event.getJobId());
    if (zoneTilingJob.isEmpty()) {
      throw new ApiException(
          SERVER_EXCEPTION, String.format("The job %s is not found", event.getJobId()));
    }
    ZoneTilingJob ZTJ = zoneTilingJob.get();
    JobStatus jobStatus = ZTJ.getStatus();
    if (FINISHED.equals(jobStatus.getProgression()) && SUCCEEDED.equals(jobStatus.getHealth())) {
      zoneService.processZoneDetectionJobs(event.getCreateFullDetection(), ZTJ);
    }
    service.accept(event);
  }
}
