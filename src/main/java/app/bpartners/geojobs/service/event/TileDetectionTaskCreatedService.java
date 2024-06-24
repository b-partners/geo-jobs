package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.service.event.TileDetectionTaskCreatedConsumer.withNewStatus;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreatedFailed;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskCreatedService implements Consumer<TileDetectionTaskCreated> {
  private final TileDetectionTaskStatusService tileDetectionTaskStatusService;
  private final TileDetectionTaskCreatedConsumer tileDetectionTaskConsumer;
  private final EventProducer eventProducer;
  private final ExceptionToStringFunction exceptionToStringFunction;

  @Override
  public void accept(TileDetectionTaskCreated tileDetectionTaskCreated) {
    TileDetectionTask tileDetectionTask = tileDetectionTaskCreated.getTileDetectionTask();
    List<DetectableType> detectableTypes = tileDetectionTaskCreated.getDetectableTypes();
    String zoneDetectionJobId = tileDetectionTaskCreated.getZoneDetectionJobId();
    tileDetectionTaskStatusService.process(tileDetectionTask);

    try {
      tileDetectionTaskConsumer.accept(tileDetectionTaskCreated);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              new TileDetectionTaskCreatedFailed(
                  new TileDetectionTaskCreated(
                      zoneDetectionJobId,
                      withNewStatus(
                          tileDetectionTask,
                          PROCESSING,
                          UNKNOWN,
                          exceptionToStringFunction.apply(e)),
                      detectableTypes),
                  1)));
      return;
    }
    eventProducer.accept(List.of(new TileDetectionTaskSucceeded(tileDetectionTask)));
  }
}
