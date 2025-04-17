package app.bpartners.geojobs.endpoint.rest.mapper;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionFromStatisticRestMapper
    implements TriFunction<
        Detection,
        TaskStatistic,
        DetectionStepName,
        app.bpartners.geojobs.endpoint.rest.model.Detection> {
  private final BucketComponent bucketComponent;
  private final DetectionStepStatisticMapper detectionStepStatisticMapper;

  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(
      Detection detection, TaskStatistic statistic, DetectionStepName detectionStepName) {
    var features =
        detection.getProvidedGeoJsonZone() == null ? null : detection.getProvidedGeoJsonZone();
    var excelUrl = bucketComponent.presign(detection.getExcelFileKey());
    var shapeUrl = bucketComponent.presign(detection.getShapeFileKey());
    var geojsonUrl = bucketComponent.presign(detection.getGeojsonS3FileKey());
    var imageUrl = bucketComponent.presign(detection.getImageFileKey());
    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .excelUrl(excelUrl)
        .shapeUrl(shapeUrl)
        .geoJsonZone(features)
        .geoJsonUrl(geojsonUrl)
        .imageUrl(imageUrl)
        .geoServerProperties(detection.getGeoServerProperties())
        .detectableObjectModel(detection.getDetectableObjectModel())
        .step(detectionStepStatisticMapper.toRestDetectionStepStatus(statistic, detectionStepName));
  }

  public app.bpartners.geojobs.endpoint.rest.model.Detection computeEmptyStatisticFromStep(
      Detection detection,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus,
      DetectionStepName detectionStepName) {
    var geoJobType = fromDetectionStep(detectionStepName);
    var emptyStatistic =
        TaskStatistic.builder()
            .jobType(geoJobType)
            .actualJobStatus(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .creationDatetime(now())
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .jobType(geoJobType)
                    .build())
            .updatedAt(now())
            .taskStatusStatistics(List.of())
            .build();
    return apply(detection, emptyStatistic, detectionStepName);
  }

  private GeoJobType fromDetectionStep(DetectionStepName stepName) {
    return switch (stepName) {
      case TILING -> GeoJobType.TILING;
      case CONFIGURING -> GeoJobType.CONFIGURING;
      case MACHINE_DETECTION, HUMAN_DETECTION -> GeoJobType.DETECTION;
      case GEO_JSON_CONVERSION -> GeoJobType.GEO_JSON_CONVERSION;
    };
  }
}
