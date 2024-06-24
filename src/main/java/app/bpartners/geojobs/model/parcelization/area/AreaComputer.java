package app.bpartners.geojobs.model.parcelization.area;

import java.util.function.Function;
import org.locationtech.jts.geom.Geometry;

public class AreaComputer implements Function<Geometry, Area> {

  @Override
  public Area apply(Geometry geometry) {
    return new SquareDegree(geometry.getArea());
  }
}
