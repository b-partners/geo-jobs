package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ParcelDetectionStatusRecomputingSubmittedService
    implements Consumer<ParcelDetectionStatusRecomputingSubmitted> {
  private final JobStatusRecomputingSubmittedService<
          ParcelDetectionJob, TileDetectionTask, ParcelDetectionStatusRecomputingSubmitted>
      service;

  public ParcelDetectionStatusRecomputingSubmittedService(
      ParcelDetectionJobService jobService,
      EventProducer eventProducer,
      TaskStatusService<TileDetectionTask> taskStatusService,
      TaskRepository<TileDetectionTask> taskRepository) {
    this.service =
        new JobStatusRecomputingSubmittedService<>(
            eventProducer, jobService, taskStatusService, taskRepository);
  }

  @Override
  public void accept(ParcelDetectionStatusRecomputingSubmitted event) {
    service.accept(event);
  }
}
