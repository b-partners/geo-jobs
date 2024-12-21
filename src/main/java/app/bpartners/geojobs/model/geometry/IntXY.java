package app.bpartners.geojobs.model.geometry;

import org.locationtech.jts.geom.Coordinate;

public record IntXY(int x, int y) {
  public int compareTo(IntXY that) {
    return new Coordinate(x, y).compareTo(new Coordinate(that.x, that.y));
  }
}
