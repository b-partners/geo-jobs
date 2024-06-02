package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.*;

@Generated("EventBridge")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode
@ToString
public class CreateAnnotatedTaskExtracted implements Serializable {
  @JsonProperty("annotationJobId")
  private String annotationJobId;

  @JsonProperty("createAnnotatedTask")
  private CreateAnnotatedTask createAnnotatedTask;
}
