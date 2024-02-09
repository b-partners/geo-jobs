package app.bpartners.geojobs.repository.annotator.gen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Task {
  private String id;
  private String userId;
  private TaskStatus status;
  private String imageUri;
  private String filename;
}
