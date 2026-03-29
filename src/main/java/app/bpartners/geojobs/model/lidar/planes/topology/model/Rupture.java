package app.bpartners.geojobs.model.lidar.planes.topology.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class Rupture {
  private final LineString line;
  private final List<Coordinate> points;
  private final List<Coordinate> endIntersection;
  private final List<Coordinate> startIntersection;
}
