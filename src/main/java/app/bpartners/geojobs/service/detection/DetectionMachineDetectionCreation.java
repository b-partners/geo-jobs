package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;

import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
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
}
