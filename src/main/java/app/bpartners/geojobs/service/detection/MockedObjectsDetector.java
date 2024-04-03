package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_CONFIDENCE_PROPERTY;
import static app.bpartners.geojobs.service.detection.DetectionResponse.REGION_LABEL_PROPERTY;

import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "true")
public class MockedObjectsDetector implements ObjectsDetector {
  public MockedObjectsDetector(
      @Value("${objects.detector.mock.maxCallDuration}") long maxCallDurationInMillis,
      @Value("${objects.detector.mock.failureRate}") double failureRate) {}

  private DetectionResponse aMockedDetectionResponse(
      Double confidence, DetectableType detectableType) {
    double randomX = Math.random() * 100;
    double randomY = Math.random() * 100;
    return DetectionResponse.builder()
        .rstImageUrl("dummyImageUrl")
        .srcImageUrl("dummyImageUrl")
        .rstRaw(
            Map.of(
                "dummyRstRawProperty",
                DetectionResponse.ImageData.builder()
                    .regions(
                        Map.of(
                            "dummyRegionProperty",
                            DetectionResponse.ImageData.Region.builder()
                                .regionAttributes(
                                    Map.of(
                                        REGION_CONFIDENCE_PROPERTY,
                                        confidence.toString(),
                                        REGION_LABEL_PROPERTY,
                                        detectableType.toString()))
                                .shapeAttributes(
                                    DetectionResponse.ImageData.ShapeAttributes.builder()
                                        .allPointsX(List.of(BigDecimal.valueOf(randomX)))
                                        .allPointsY(List.of(BigDecimal.valueOf(randomY)))
                                        .build())
                                .build()))
                    .build()))
        .build();
  }

  @Override
  public DetectionResponse apply(DetectionTask task, List<DetectableType> detectableTypes) {
    double randomConfidence = Math.random();
    return aMockedDetectionResponse(randomConfidence, DetectableType.ROOF);
  }
}
