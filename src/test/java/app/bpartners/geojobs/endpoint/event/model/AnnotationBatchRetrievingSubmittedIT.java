package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTaskRetrievingSubmitted;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationBatchRetrievingSubmittedIT extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var taskRetrievingSubmitted =
        new AnnotationTaskRetrievingSubmitted(
            "humanZdjId", "annotationRetrievingJobId", "annotationJobId", 1024);

    var serialized = om.writeValueAsString(taskRetrievingSubmitted);
    var deserialized = om.readValue(serialized, AnnotationTaskRetrievingSubmitted.class);

    assertEquals(taskRetrievingSubmitted, deserialized);
    assertEquals("humanZdjId", deserialized.getHumanZdjId());
    assertEquals("annotationJobId", deserialized.getAnnotationJobId());
    assertEquals(1024, deserialized.getImageWidth());
    assertEquals(Duration.ofMinutes(10), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
