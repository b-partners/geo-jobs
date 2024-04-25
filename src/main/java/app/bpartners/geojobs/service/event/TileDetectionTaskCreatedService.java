package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.TileDetectionTaskCreatedFailed;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TileDetectionTaskCreatedService implements Consumer<TileDetectionTaskCreated> {
  private final TileDetectionTaskCreatedConsumer tileDetectionTaskConsumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(TileDetectionTaskCreated tileDetectionTaskCreated) {
    // TODO: set status progression update here
    try {
      tileDetectionTaskConsumer.accept(tileDetectionTaskCreated);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              // TODO: must be with new status PROCESSING
              new TileDetectionTaskCreatedFailed(tileDetectionTaskCreated, 1)));
    }
    // TODO: succeed TileDetectionTask here
    // /!\ When all TileDetectionTask are succeeded, only there detectionTask is succeeded
  }
}
