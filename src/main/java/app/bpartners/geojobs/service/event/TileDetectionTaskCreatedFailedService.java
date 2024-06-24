package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreatedFailed;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskCreatedFailedService
    implements Consumer<TileDetectionTaskCreatedFailed> {
  private static final int MAX_ATTEMPT = 3;
  private final EventProducer eventProducer;
  private final TileDetectionTaskCreatedConsumer consumer;
  private final TileDetectionTaskStatusService tileDetectionTaskStatusService;
  private final ExceptionToStringFunction exceptionToStringFunction;

  @Override
  public void accept(TileDetectionTaskCreatedFailed tileDetectionTaskCreatedFailed) {
    var createdTask = tileDetectionTaskCreatedFailed.getTileDetectionTaskCreated();
    var zoneDetectionJobId = createdTask.getZoneDetectionJobId();
    var tileDetectionTask = createdTask.getTileDetectionTask();
    var attemptNb = tileDetectionTaskCreatedFailed.getAttemptNb();
    var detectableTypes = createdTask.getDetectableTypes();
    if (attemptNb > MAX_ATTEMPT) {
      tileDetectionTaskStatusService.fail(tileDetectionTask);
      log.info(
          "Max attempt reached for tileDetectionTaskCreatedFailed={}",
          tileDetectionTaskCreatedFailed);
      return;
    }
    try {
      consumer.accept(createdTask);
    } catch (Exception e) {
      var newTask =
          new TileDetectionTaskCreated(
              zoneDetectionJobId,
              TileDetectionTaskCreatedConsumer.withNewStatus(
                  tileDetectionTask, PROCESSING, UNKNOWN, exceptionToStringFunction.apply(e)),
              detectableTypes);
      eventProducer.accept(List.of(new TileDetectionTaskCreatedFailed(newTask, attemptNb + 1)));
      return;
    }
    eventProducer.accept(
        List.of(
            TileDetectionTaskSucceeded.builder()
                .tileDetectionTask(createdTask.getTileDetectionTask())
                .build()));
  }
}
