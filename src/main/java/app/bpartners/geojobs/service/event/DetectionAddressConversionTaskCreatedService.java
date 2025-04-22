package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.service.DetectionAddressConversionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskCreated;
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
  private final DetectionAddressConversionTaskConsumer taskConsumer;
  private final DetectionAddressConversionTaskStatusService taskStatusService;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionAddressConversionTaskCreated event) {
    var task = event.getTask();
    taskStatusService.process(task);

    taskConsumer.accept(task);

    eventProducer.accept(
        List.of(
            DetectionAddressConversionTaskSucceeded.builder()
                .succeededTask(withNewStatus(task, FINISHED, SUCCEEDED, null))
                .build()));
  }
}
