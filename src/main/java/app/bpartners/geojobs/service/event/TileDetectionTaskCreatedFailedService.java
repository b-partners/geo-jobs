package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskCreatedFailed;
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
  private final TileDetectionTaskCreatedConsumer tileDetectionTaskConsumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(TileDetectionTaskCreatedFailed tileDetectionTaskCreatedFailed) {
    var tileDetectionTaskCreated = tileDetectionTaskCreatedFailed.getTileDetectionTaskCreated();
    var attemptNb = tileDetectionTaskCreatedFailed.getAttemptNb();
    if (attemptNb > MAX_ATTEMPT) {
      // TODO: taskStatusService.fail(task);
      log.error(
          "Max attempt reached for TileDetectionTask(taskId={})",
          tileDetectionTaskCreated.getTileDetectionTask().getTaskId());
      return;
    }
    try {
      tileDetectionTaskConsumer.accept(tileDetectionTaskCreated);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              // TODO: must be with new status PROCESSING
              new TileDetectionTaskCreatedFailed(tileDetectionTaskCreated, attemptNb + 1)));
      return;
    }
    // TODO: succeed TileDetectionTask here
    // /!\ When all TileDetectionTask are succeeded, only there detectionTask is succeeded
  }
}
