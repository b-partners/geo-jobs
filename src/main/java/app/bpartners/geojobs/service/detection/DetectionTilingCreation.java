package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
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

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(Detection detection) {
    var ztj = processZoneTilingJob(detection);
    var detectionWithZTJ =
        detectionRepository.save(detection.toBuilder().ztjId(ztj.getId()).build());
    return detectionTilingStatisticsComputer.apply(detectionWithZTJ, ztj.getId());
  }

  private ZoneTilingJob processZoneTilingJob(Detection detection) {
    var createJob = zoneTilingJobMapper.from(detection);
    var job = zoneTilingJobMapper.toDomain(createJob);
    var tilingTasks = getTilingTasks(createJob, job.getId());

    return zoneTilingJobService.create(job, tilingTasks);
  }
}
