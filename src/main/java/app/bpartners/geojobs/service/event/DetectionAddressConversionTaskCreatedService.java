package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.DetectionAddressConversionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskFailed;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskSucceeded;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskConsumer;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskStatusService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionTaskCreatedService
    implements Consumer<DetectionAddressConversionTaskCreated> {
  private static final int MAX_ATTEMPT_NB = 5;
  private final DetectionAddressConversionTaskConsumer taskConsumer;
  private final DetectionAddressConversionTaskStatusService taskStatusService;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionAddressConversionTaskCreated event) {
    var task = event.getTask();
    var actualAttemptNb = event.getAttemptNb();
    var newAttemptNb = actualAttemptNb + 1;
    if (actualAttemptNb == 1) {
      taskStatusService.process(task);
    }
    if (newAttemptNb > MAX_ATTEMPT_NB) {
      eventProducer.accept(
          List.of(
              DetectionAddressConversionTaskFailed.builder()
                  .task(withNewStatus(task, FINISHED, FAILED, null))
                  .build()));
      return;
    }
    try {
      taskConsumer.accept(task, event.getE2ApiKey());
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              DetectionAddressConversionTaskCreated.builder()
                  .e2ApiKey(event.getE2ApiKey())
                  .task(task)
                  .attemptNb(newAttemptNb)
                  .build()));
      return;
    }
    eventProducer.accept(
        List.of(
            DetectionAddressConversionTaskSucceeded.builder()
                .succeededTask(withNewStatus(task, FINISHED, SUCCEEDED, null))
                .build()));
  }
}
