package app.bpartners.geojobs.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class TilingTaskSucceededIT extends FacadeIT {
  @Autowired ObjectMapper om;
  @MockBean Parcel parcel;
  @MockBean TilingTask tilingTask;

  private TilingTask tilingTask() {
    return TilingTask.builder()
        .parcels(List.of(Parcel.builder().parcelContent(ParcelContent.builder().build()).build()))
        .build();
  }

  private CreateFullDetection fullDetection() {
    return new CreateFullDetection();
  }

  @Test
  void serialize_then_deserialize() throws JsonProcessingException {
    var tilingTaskSucceeded = new TilingTaskSucceeded(tilingTask(), fullDetection());

    var serialized = om.writeValueAsString(tilingTaskSucceeded);
    var deserialized = om.readValue(serialized, TilingTaskSucceeded.class);

    assertEquals(tilingTaskSucceeded, deserialized);
    assertEquals(tilingTask(), deserialized.getTask());
    assertEquals(fullDetection(), deserialized.getFullDetection());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), deserialized.maxConsumerBackoffBetweenRetries());
  }
}
