package app.bpartners.geojobs.repository.annotator.gen;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class Polygon {
  private List<Point> points;
}
