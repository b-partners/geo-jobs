package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
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
