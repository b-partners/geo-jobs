package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationRetrievingJobStatusRecomputingSubmitted;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AnnotationRetrievingJobStatusRecomputingSubmittedTest extends FacadeIT {

  @Autowired ObjectMapper om;

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var retrievingJob =
        new AnnotationRetrievingJobStatusRecomputingSubmitted("annotationRetrievingJobId");

    var serialized = om.writeValueAsString(retrievingJob);
    var deserialized =
        om.readValue(serialized, AnnotationRetrievingJobStatusRecomputingSubmitted.class);

    assertEquals(retrievingJob, deserialized);
    assertEquals("annotationRetrievingJobId", deserialized.getAnnotationRetrievingJobId());
    assertEquals(Duration.ofMinutes(5), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
