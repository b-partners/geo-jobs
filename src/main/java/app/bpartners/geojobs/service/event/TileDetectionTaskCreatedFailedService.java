package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.TileDetectionTaskCreatedConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreatedFailed;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskToTaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
@Slf4j
public class TileDetectionTaskCreatedFailedService
    implements Consumer<TileDetectionTaskCreatedFailed> {
  private static final int MAX_ATTEMPT = 3;
  private final RetryableTaskToTaskStatusService<TileDetectionTask, DetectionTask, ZoneDetectionJob>
      taskToTaskStatusService;
  private final TileDetectionTaskCreatedConsumer tileDetectionTaskConsumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(TileDetectionTaskCreatedFailed tileDetectionTaskCreatedFailed) {
    var tileDetectionTaskCreated = tileDetectionTaskCreatedFailed.getTileDetectionTaskCreated();
    var tileDetectionTask = tileDetectionTaskCreated.getTileDetectionTask();
    var detectableTypes = tileDetectionTaskCreated.getDetectableTypes();
    var attemptNb = tileDetectionTaskCreatedFailed.getAttemptNb();
    if (attemptNb > MAX_ATTEMPT) {
      taskToTaskStatusService.fail(tileDetectionTask);
      log.error(
          "Max attempt reached for TileDetectionTask(taskId={})",
          tileDetectionTask.getDetectionTaskId());
      return;
    }
    try {
      tileDetectionTaskConsumer.accept(tileDetectionTaskCreated);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new TileDetectionTaskCreatedFailed(
                  new TileDetectionTaskCreated(
                      withNewStatus(tileDetectionTask, PROCESSING, UNKNOWN, e.getMessage()),
                      detectableTypes),
                  attemptNb + 1)));
      return;
    }
    eventProducer.accept(List.of(new TileDetectionTaskSucceeded(tileDetectionTask)));
  }
}
