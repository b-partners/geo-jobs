package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class Polygon {
    private List<Point> points;
}
