package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.ROOF_HEIGHT_PROPERTY_NAME;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.ROOF_SLOPE_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectionStepStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.DetectionFeaturesResultImageRetriever;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DetectionFromStatisticRestMapperTest {
  BucketComponent bucketComponentMock = mock();
  DetectionStepStatisticMapper detectionStepStatisticMapperMock = mock();
  DetectionFeaturesResultImageRetriever detectionFeaturesResultImageRetrieverMock = mock();

  DetectionFromStatisticRestMapper subject =
      new DetectionFromStatisticRestMapper(
          bucketComponentMock,
          detectionStepStatisticMapperMock,
          detectionFeaturesResultImageRetrieverMock);

  private static Detection detectionWithFeatureDelimitation(double slope, double height) {
    HashMap<String, Object> properties =
        new HashMap<>(Map.of(ROOF_SLOPE_PROPERTY_NAME, slope, ROOF_HEIGHT_PROPERTY_NAME, height));
    var feature = Feature.builder().properties(properties).build();
    var featureDelimitation = new FeatureWithDelimitation(feature, List.of(feature));

    return Detection.builder()
        .polygonRoofDelimitation(mock())
        .vggFileKey("vgg-key")
        .shapeFileKey("shape-key")
        .geojsonS3FileKey("geojson-key")
        .imageFileKey("image-key")
        .pdfFileKey("pdf-key")
        .vggFileKey("vgg-key")
        .featureWithDelimitations(List.of(featureDelimitation))
        .build();
  }

  @Test
  void apply_detection_from_statistic_rest_mapper_ok() {
    var expectedSlope = 45d;
    var expectedHeight = 5d;
    var detection = detectionWithFeatureDelimitation(expectedSlope, expectedHeight);

    when(detectionFeaturesResultImageRetrieverMock.apply(any())).thenReturn(List.of());
    when(bucketComponentMock.presign(any())).thenReturn("https://dummy.com");
    when(detectionStepStatisticMapperMock.toRestDetectionStepStatus(any(), any()))
        .thenReturn(mock());

    var actualDetection = subject.apply(detection, mock(), mock());

    assertNotNull(actualDetection.getRoofDelimiter());
    assertNotNull(actualDetection.getRoofDelimiter().getRoofSlopeInDegree());
    assertNotNull(actualDetection.getRoofDelimiter().getRoofHeightInMeter());

    assertEquals(
        expectedSlope, actualDetection.getRoofDelimiter().getRoofSlopeInDegree().doubleValue());
    assertEquals(
        expectedSlope, actualDetection.getRoofDelimiter().getRoofSlopeInDegree().doubleValue());
  }
}
