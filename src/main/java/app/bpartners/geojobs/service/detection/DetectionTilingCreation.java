package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RooferMadeDetectionCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.tiling.RooferMadeTilingService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
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
  private final RooferMadeTilingService rooferMadeTilingService;
  private final EventProducer<RooferMadeDetectionCreated> eventProducer;
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(Detection detection) {
    var ztj = processZoneTilingJob(detection);
    String zdjId = null;
    if (ztj.isRooferMade()) {
      var zdj = zoneDetectionJobService.saveZDJFromZTJ(ztj);
      eventProducer.accept(
          List.of(
              RooferMadeDetectionCreated.builder()
                  .zdjId(zdj.getId())
                  .detectionId(detection.getId())
                  .build()));
      zdjId = zdj.getId();
    }
    var detectionWithZTJ =
        detectionRepository.save(detection.toBuilder().ztjId(ztj.getId()).zdjId(zdjId).build());
    return detectionTilingStatisticsComputer.apply(detectionWithZTJ, ztj.getId());
  }

  private ZoneTilingJob processZoneTilingJob(Detection detection) {
    var createJob = zoneTilingJobMapper.from(detection);
    var job = zoneTilingJobMapper.toDomain(createJob, detection.isRooferMade());
    var tilingTasks = getTilingTasks(createJob, job.getId());
    if (job.isRooferMade()) {
      return rooferMadeTilingService.apply(job, tilingTasks);
    }
    return zoneTilingJobService.create(job, tilingTasks);
  }
}
