package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.ParcelDetectionTaskConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskFailed;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@Service
public class ParcelDetectionTaskFailedService implements Consumer<ParcelDetectionTaskFailed> {

  private final TaskStatusService<ParcelDetectionTask> taskStatusService;
  private final ParcelDetectionTaskConsumer parcelDetectionTaskConsumer;
  private final EventProducer eventProducer;
  private final ExceptionToStringFunction exceptionToStringFunction;

  private static final int MAX_ATTEMPT = 3;

  // TODO: the TileDetectionTask must be the one that update detection task status
  @Override
  public void accept(ParcelDetectionTaskFailed parcelDetectionTaskFailed) {
    var task = parcelDetectionTaskFailed.getTask();
    var attemptNb = parcelDetectionTaskFailed.getAttemptNb();
    if (attemptNb > MAX_ATTEMPT) {
      taskStatusService.fail(task);
      log.info("Max attempt reached for detectionTaskFailed={}", parcelDetectionTaskFailed);
      return;
    }

    try {
      parcelDetectionTaskConsumer.accept(task);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new ParcelDetectionTaskFailed(
                  withNewStatus(task, PROCESSING, UNKNOWN, exceptionToStringFunction.apply(e)),
                  attemptNb + 1)));
    }
  }
}
