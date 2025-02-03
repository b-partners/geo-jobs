package app.bpartners.geojobs.endpoint.rest.mapper;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectionStepName;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.repository.model.detection.Detection;
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
    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .excelUrl(excelUrl)
        .shapeUrl(shapeUrl)
        .geoJsonZone(features)
        .geoJsonUrl(geojsonUrl)
        .geoServerProperties(detection.getGeoServerProperties())
        .detectableObjectModel(detection.getDetectableObjectModel())
        .step(detectionStepStatisticMapper.toRestDetectionStepStatus(statistic, detectionStepName));
  }
}
