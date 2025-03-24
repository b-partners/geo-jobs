package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.TilingTaskConsumer;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionTilingCreation
    implements Function<Detection, app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final ZoneTilingJobMapper zoneTilingJobMapper;
  private final ZoneTilingJobService zoneTilingJobService;
  private final DetectionRepository detectionRepository;
  private final DetectionTilingStatisticsComputer detectionTilingStatisticsComputer;
  private final TilingTaskConsumer tilingTaskConsumer;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() - 1));

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(Detection detection) {
    var ztj = processZoneTilingJob(detection);
    var detectionWithZTJ =
        detectionRepository.save(detection.toBuilder().ztjId(ztj.getId()).build());
    return detectionTilingStatisticsComputer.apply(detectionWithZTJ, ztj.getId());
  }

  private ZoneTilingJob processZoneTilingJob(Detection detection) {
    var createJob = zoneTilingJobMapper.from(detection);
    var job = zoneTilingJobMapper.toDomain(createJob, detection.isRooferMade());
    var tilingTasks = getTilingTasks(createJob, job.getId());
    if (job.isRooferMade()) {
      try {
        executorService.invokeAll(
            tilingTasks.stream()
                .map(
                    task ->
                        ((Callable<ParcelTilingTask>)
                            () -> {
                              tilingTaskConsumer.accept(task);
                              return task;
                            }))
                .collect(toSet()));
        return zoneTilingJobService.create(job, tilingTasks);
      } catch (InterruptedException e) {
        throw new ApiException(SERVER_EXCEPTION, e.getMessage());
      }
    }
    return zoneTilingJobService.create(job, tilingTasks);
  }
}
