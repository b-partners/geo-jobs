package app.bpartners.geojobs.endpoint.event.model;

import static app.bpartners.geojobs.endpoint.event.EventStack.EVENT_STACK_4;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DetectionRoofSlopeAndHeightRequestedIT extends FacadeIT {
  @Autowired ObjectMapper om;

  private DetectionLidarAnalysisRequested detectionLidarAnalysisRequested() {
    return DetectionLidarAnalysisRequested.builder().detectionId("detection-id").build();
  }

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var original = detectionLidarAnalysisRequested();

    var serialized = om.writeValueAsString(original);
    var deserialized = om.readValue(serialized, DetectionLidarAnalysisRequested.class);

    assertEquals(original, deserialized);
    assertEquals(Duration.ofMinutes(20), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(2), deserialized.maxConsumerBackoffBetweenRetries());
    assertEquals(EVENT_STACK_4, deserialized.getEventStack());
  }
}
