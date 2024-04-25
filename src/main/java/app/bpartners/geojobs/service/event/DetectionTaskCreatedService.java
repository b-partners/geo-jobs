package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.DetectionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskFailed;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionTaskCreatedService implements Consumer<DetectionTaskCreated> {
  private final RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusService;
  private final DetectionTaskConsumer detectionTaskConsumer;
  private final EventProducer eventProducer;

  // TODO: the TileDetectionTask must be the one that update detection task status
  @Override
  public void accept(DetectionTaskCreated detectionTaskCreated) {
    var task = detectionTaskCreated.getTask();
    taskStatusService.process(task);

    try {
      detectionTaskConsumer.accept(task);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new DetectionTaskFailed(
                  withNewStatus(task, PROCESSING, UNKNOWN, e.getMessage()), 1)));
    }
    eventProducer.accept(
        List.of(new DetectionTaskSucceeded(withNewStatus(task, FINISHED, SUCCEEDED, null))));
  }
}
