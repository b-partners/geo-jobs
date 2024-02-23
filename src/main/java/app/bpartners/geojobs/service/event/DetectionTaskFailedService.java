package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.DetectionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskFailed;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class DetectionTaskFailedService implements Consumer<DetectionTaskFailed> {

  private final RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusService;
  private final DetectionTaskConsumer detectionTaskConsumer;
  private final EventProducer eventProducer;

  private static final int MAX_ATTEMPT = 3;

  @Override
  public void accept(DetectionTaskFailed detectionTaskFailed) {
    var task = detectionTaskFailed.getTask();
    var attemptNb = detectionTaskFailed.getAttemptNb();
    if (attemptNb > MAX_ATTEMPT) {
      taskStatusService.fail(task);
      log.error("Max attempt reached for detectionTaskFailed={}", detectionTaskFailed);
      return;
    }

    try {
      detectionTaskConsumer.accept(task);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new DetectionTaskFailed(
                  withNewStatus(task, PROCESSING, UNKNOWN, e.getMessage()), attemptNb + 1)));
      return;
    }

    eventProducer.accept(
        List.of(new DetectionTaskSucceeded(withNewStatus(task, FINISHED, SUCCEEDED, null))));
  }
}
