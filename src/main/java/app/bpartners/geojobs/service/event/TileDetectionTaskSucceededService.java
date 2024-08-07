package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.endpoint.event.model.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
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
  private final EventProducer eventProducer;

  @Override
  public void accept(TileDetectionTaskSucceeded tileDetectionTaskSucceeded) {
    var tileDetectionTask = tileDetectionTaskSucceeded.getTileDetectionTask();
    tileDetectionTaskRepository.save(tileDetectionTask);
    tileDetectionTaskStatusService.succeed(tileDetectionTask);
    List<TileDetectionTask> tasks =
        tileDetectionTaskRepository.findAllByJobId(tileDetectionTask.getJobId());
    if (areFinished(tasks)) {
      log.info("Tile detection tasks from jobId: {} are finished", tileDetectionTask.getJobId());
      eventProducer.accept(
          List.of(
              new ZDJParcelsStatusRecomputingSubmitted(tileDetectionTaskSucceeded.getZdjId()),
              new ZDJStatusRecomputingSubmitted(tileDetectionTaskSucceeded.getZdjId()),
              new AutoTaskStatisticRecomputingSubmitted(tileDetectionTaskSucceeded.getZdjId())));
    }
  }

  public boolean areFinished(List<TileDetectionTask> tasks) {
    return tasks.stream().allMatch(Task::isSucceeded);
  }
}
