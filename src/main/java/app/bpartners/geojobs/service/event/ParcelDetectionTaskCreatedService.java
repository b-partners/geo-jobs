package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.ParcelDetectionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskFailed;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ParcelDetectionTaskCreatedService implements Consumer<ParcelDetectionTaskCreated> {
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;
  private final ParcelDetectionTaskConsumer parcelDetectionTaskConsumer;
  private final EventProducer eventProducer;
  private final ExceptionToStringFunction exceptionToStringFunction;

  @Override
  public void accept(ParcelDetectionTaskCreated parcelDetectionTaskCreated) {
    var task = parcelDetectionTaskCreated.getTask();
    taskStatusService.process(task);

    try {
      parcelDetectionTaskConsumer.accept(task);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new ParcelDetectionTaskFailed(
                  withNewStatus(task, PROCESSING, UNKNOWN, exceptionToStringFunction.apply(e)),
                  1)));
    }
  }
}
