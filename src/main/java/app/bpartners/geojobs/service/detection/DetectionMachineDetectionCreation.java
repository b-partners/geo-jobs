package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;

import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ParcelTilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionMachineDetectionCreation
    implements BiFunction<
        Detection, ZoneTilingJob, app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneDetectionJobValidator detectionJobValidator;
  private final DetectionMachineDetectionStatisticsComputer
      detectionMachineDetectionStatisticsComputer;
  private final DetectionRepository detectionRepository;

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(
      Detection detection, ZoneTilingJob zoneTilingJob) {
    var zoneDetectionJob = zoneDetectionJobService.getByTilingJobId(zoneTilingJob.getId(), MACHINE);

    detectionJobValidator.accept(zoneDetectionJob.getId());

    var savedZoneDetectionJob =
        zoneDetectionJobService.processZDJ(
            zoneDetectionJob.getId(), detection.getDetectableObjectConfigurations());

    return detectionMachineDetectionStatisticsComputer.apply(
        detection, savedZoneDetectionJob.getId());
  }

  public void processMachineDetection(
      Detection detection, ZoneTilingJob zoneTilingJob, List<ParcelTilingTask> tilingTasks) {
    var zoneDetectionJob = zoneDetectionJobService.saveZDJFromZTJ(zoneTilingJob);
    detectionRepository.save(detection.toBuilder().zdjId(zoneDetectionJob.getId()).build());

    var tileDetectionTasks =
        tilingTasks.stream()
            .map(
                task ->
                    task.getTiles().stream()
                        .map(
                            tile -> {
                              TileDetectionTask tileDetectionTask =
                                  new TileDetectionTask(
                                      null, null, null, null, tile, new ArrayList<>());
                              tileDetectionTask.setZoneDetectionJobId(zoneDetectionJob.getId());
                              tileDetectionTask.setDetectableObjectConfigurations(
                                  detection.getDetectableObjectConfigurations());
                              return tileDetectionTask;
                            })
                        .toList())
            .flatMap(List::stream)
            .toList();

    zoneDetectionJobService.consumeTasks(tileDetectionTasks);
  }
}
