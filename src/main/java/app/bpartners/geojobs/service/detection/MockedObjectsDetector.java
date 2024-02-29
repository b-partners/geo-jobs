package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.service.FalliblyDurableMockedFunction;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "objects.detector.mock.activated", havingValue = "false")
public class MockedObjectsDetector
    extends FalliblyDurableMockedFunction<DetectionTask, DetectionResponse>
    implements ObjectsDetector {
  public MockedObjectsDetector(
      @Value("${objects.detector.mock.maxCallDuration}") long maxCallDurationInMillis,
      @Value("${objects.detector.mock.failureRate}") double failureRate) {
    super(Duration.ofMillis(maxCallDurationInMillis), failureRate);
  }

  @Override
  protected DetectionResponse successfulMockedApply(DetectionTask task) {
    return DetectionResponse.builder()
        .rstImageUrl("dummyImageUrl")
        .srcImageUrl("dummyImageUrl")
        .rstRaw(Map.of())
        .build();
  }
}
