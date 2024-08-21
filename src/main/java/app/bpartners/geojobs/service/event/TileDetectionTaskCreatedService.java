package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
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

  @Override
  public void accept(TileDetectionTaskCreated tileDetectionTaskCreated) {
    TileDetectionTask tileDetectionTask = tileDetectionTaskCreated.getTileDetectionTask();
    tileDetectionTaskStatusService.process(tileDetectionTask);

    tileDetectionTaskConsumer.accept(tileDetectionTaskCreated);

    eventProducer.accept(
        List.of(
            new TileDetectionTaskSucceeded(
                tileDetectionTask, tileDetectionTaskCreated.getZoneDetectionJobId())));
  }
}
