package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParcelDetectionStatusRecomputingSubmittedService
    extends JobStatusRecomputingSubmittedService<
        ParcelDetectionJob, TileDetectionTask, ParcelDetectionStatusRecomputingSubmitted> {

  public ParcelDetectionStatusRecomputingSubmittedService(
      JobService<TileDetectionTask, ParcelDetectionJob> jobService,
      TaskStatusService<TileDetectionTask> taskStatusService,
      TaskRepository<TileDetectionTask> taskRepository) {
    super(jobService, taskStatusService, taskRepository);
  }
}
