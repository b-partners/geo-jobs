package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationTaskRetrievingSubmittedIT extends FacadeIT {
  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var batchRetrievingSubmitted =
        new AnnotationBatchRetrievingSubmitted(
            "jobId", "annotationJobId", "annotationTaskId", 1024, 2000, 5000, 20);

    var serialized = om.writeValueAsString(batchRetrievingSubmitted);
    var deserialized = om.readValue(serialized, AnnotationBatchRetrievingSubmitted.class);

    assertEquals(batchRetrievingSubmitted, deserialized);
    assertEquals("jobId", deserialized.getJobId());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
    assertEquals(Duration.ofMinutes(5), deserialized.maxConsumerDuration());
  }
}
