package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionMachineDetectionStatisticsComputer
    implements BiFunction<Detection, String, app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(
      Detection detection, String zdjId) {
    if (detection.isRooferMade()) {
      var statistics = zoneDetectionJobService.getTaskStatistic(zdjId);
      if (detection.getGeojsonS3FileKey() != null) {
        statistics.setActualJobStatus(
            JobStatus.builder()
                .id(randomUUID().toString())
                .jobId(detection.getId())
                .jobType(DETECTION)
                .creationDatetime(statistics.getUpdatedAt())
                .health(SUCCEEDED)
                .progression(FINISHED)
                .build());
      }
      return detectionFromStatisticRestMapper.apply(detection, statistics, MACHINE_DETECTION);
    }
    return detectionFromStatisticRestMapper.apply(
        detection, zoneDetectionJobService.computeTaskStatistics(zdjId), MACHINE_DETECTION);
  }
}
