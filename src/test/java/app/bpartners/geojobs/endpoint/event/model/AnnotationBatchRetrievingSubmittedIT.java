package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationBatchRetrievingSubmittedIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var taskRetrievingSubmitted = new AnnotationTaskRetrievingSubmitted("jobId", "humanZdjId");

    var serialized = om.writeValueAsString(taskRetrievingSubmitted);
    var deserialized = om.readValue(serialized, AnnotationTaskRetrievingSubmitted.class);

    assertEquals(taskRetrievingSubmitted, deserialized);
    assertEquals("jobId", deserialized.getZdjId());
    assertEquals("humanZdjId", deserialized.getHumanZdjId());
    assertEquals(Duration.ofMinutes(5), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
