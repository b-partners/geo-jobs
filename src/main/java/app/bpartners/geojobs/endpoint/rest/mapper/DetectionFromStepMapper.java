package app.bpartners.geojobs.endpoint.rest.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.GEO_JSON;
import static app.bpartners.geojobs.endpoint.rest.model.GeoJsonOutput.ZIP;

import app.bpartners.geojobs.endpoint.rest.model.Detection;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.DetectionStep;
import app.bpartners.geojobs.service.DetectionFeaturesResultImageRetriever;
import app.bpartners.geojobs.service.DetectionImageAttributeRetriever;
import app.bpartners.geojobs.service.DetectionVggAttributeRetriever;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionFromStepMapper {
  private final BucketComponent bucketComponent;
  private final DetectionFeaturesResultImageRetriever featuresImageRetriever;
  private final DetectionImageAttributeRetriever imageAttributeRetriever;
  private final DetectionVggAttributeRetriever vggAttributeRetriever;
  private final DetectionFromStatisticRestMapper fromStatMapper;
  private final DetectionStepMapper detectionStepMapper;

  public Detection apply(
      app.bpartners.geojobs.repository.model.detection.Detection detection, DetectionStep step) {
    var features = featuresImageRetriever.apply(detection);
    var imageUrl = imageAttributeRetriever.apply(detection);
    var vggUrl = vggAttributeRetriever.apply(detection);
    var excelUrl = bucketComponent.presign(detection.getExcelFileKey());
    var shapeUrl = bucketComponent.presign(detection.getShapeFileKey());
    var geojsonUrl = bucketComponent.presign(detection.getGeojsonS3FileKey());
    var pdfUrl = bucketComponent.presign(detection.getPdfFileKey());
    var featuresWithHiddenProperties = fromStatMapper.hideUselessRestProperties(features);

    return new app.bpartners.geojobs.endpoint.rest.model.Detection()
        .id(detection.getEndToEndId())
        .emailReceiver(detection.getEmailReceiver())
        .zoneName(detection.getZoneName())
        .excelUrl(excelUrl)
        .shapeUrl(shapeUrl)
        .geoJsonZone(featuresWithHiddenProperties)
        .geoJsonUrl(geojsonUrl)
        .imageUrl(imageUrl)
        .pdfUrl(pdfUrl)
        .vggUrl(vggUrl)
        .geoServerProperties(detection.getGeoServerProperties())
        .geoJsonDelimitationType(detection.getGeoJsonDelimitationType())
        .detectableObjectModel(detection.getDetectableObjectModel())
        .step(detectionStepMapper.toRest(step))
        .addresses(
            detection.getConvertedAddresses() == null
                ? List.of()
                : detection.getConvertedAddresses())
        .roofDelimiter(fromStatMapper.retrieveRoofDelimiter(detection))
        .geoJsonOutput(detection.isOutputZipped() ? ZIP : GEO_JSON)
        .needsImageOutput(detection.needsImageOutput());
  }
}
