package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TileDetectionTaskSucceededService implements Consumer<TileDetectionTaskSucceeded> {
  private final TileDetectionTaskStatusService tileDetectionTaskStatusService;
  private final TileDetectionTaskRepository tileDetectionTaskRepository;

  @Override
  public void accept(TileDetectionTaskSucceeded tileDetectionTaskSucceeded) {
    var tileDetectionTask = tileDetectionTaskSucceeded.getTileDetectionTask();
    tileDetectionTaskRepository.save(tileDetectionTask);
    tileDetectionTaskStatusService.succeed(tileDetectionTask);
  }
}
