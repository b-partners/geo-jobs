package app.bpartners.geojobs.endpoint.rest.mapper;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.endpoint.rest.model.RoofDelimiter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.model.GeoJobType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionFeaturesResultImageRetriever;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;

@Slf4j
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
  private final DetectionFeaturesResultImageRetriever featuresImageRetriever;

  public app.bpartners.geojobs.endpoint.rest.model.Detection apply(
      Detection detection, TaskStatistic statistic, DetectionStepName detectionStepName) {
    var features = featuresImageRetriever.apply(detection);
    var excelUrl = bucketComponent.presign(detection.getExcelFileKey());
    var shapeUrl = bucketComponent.presign(detection.getShapeFileKey());
    var geojsonUrl = bucketComponent.presign(detection.getGeojsonS3FileKey());
    var imageUrl = bucketComponent.presign(detection.getImageFileKey());
    var pdfUrl = bucketComponent.presign(detection.getPdfFileKey());
    var vggUrl = bucketComponent.presign(detection.getVggFileKey());
    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .excelUrl(excelUrl)
        .shapeUrl(shapeUrl)
        .geoJsonZone(features)
        .geoJsonUrl(geojsonUrl)
        .imageUrl(imageUrl)
        .pdfUrl(pdfUrl)
        .vggUrl(vggUrl)
        .geoServerProperties(detection.getGeoServerProperties())
        .detectableObjectModel(detection.getDetectableObjectModel())
        .step(detectionStepStatisticMapper.toRestDetectionStepStatus(statistic, detectionStepName))
        .addresses(
            detection.getConvertedAddresses() == null
                ? List.of()
                : detection.getConvertedAddresses())
        .roofDelimiter(
            detection.getPolygonRoofDelimitation() == null
                    || detection.getPolygonRoofDelimitation().isEmpty()
                ? null
                : new RoofDelimiter().polygon(detection.getPolygonRoofDelimitation()));
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
