package app.bpartners.geojobs.repository.annotator.gen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EqualsAndHashCode
@ToString
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Polygon {
  private List<Point> points;
}
