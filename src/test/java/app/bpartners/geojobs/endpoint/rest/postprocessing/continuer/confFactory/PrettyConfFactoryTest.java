package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class PrettyConfFactoryTest {

  private static final GeometryFactory factory = new GeometryFactory();

  private LatLonPolygon createPolygon(Coordinate... coords) {
    if (!coords[0].equals2D(coords[coords.length - 1])) {
      Coordinate[] closed = new Coordinate[coords.length + 1];
      System.arraycopy(coords, 0, closed, 0, coords.length);
      closed[coords.length] = coords[0];
      coords = closed;
    }
    LinearRing ring = factory.createLinearRing(coords);
    Polygon polygon = factory.createPolygon(ring, null);
    return new LatLonPolygon(polygon);
  }

  @Test
  public void test_on_typical_polygon() {
    var polygon =
        createPolygon(
            new Coordinate(0, 0),
            new Coordinate(0, 10),
            new Coordinate(10, 10),
            new Coordinate(10, 0),
            new Coordinate(0, 0));

    var conf = new PrettyConfFactory().apply(Set.of(polygon));
    assertTrue(conf.dpbThreshold() >= 5 && conf.dpbThreshold() <= 150);
  }

  @Test
  void test_on_large_polygon() {
    var polygon =
        createPolygon(
            new Coordinate(0, 0),
            new Coordinate(0, 1000),
            new Coordinate(1000, 1000),
            new Coordinate(1000, 0),
            new Coordinate(0, 0));

    var conf = new PrettyConfFactory().apply(Set.of(polygon));
    assertTrue(conf.dpbThreshold() > 20);
  }

  @Test
  void test_small_detailed_polygon() {
    var polygon =
        createPolygon(
            new Coordinate(0, 0),
            new Coordinate(0, 1),
            new Coordinate(1, 1),
            new Coordinate(1, 0),
            new Coordinate(0, 0));

    var conf = new PrettyConfFactory().apply(Set.of(polygon));
    assertTrue(conf.dpbThreshold() <= 20);
  }

  @Test
  void test_degenerate_polygon() {
    var polygon = createPolygon(new Coordinate(0, 0), new Coordinate(0, 0), new Coordinate(0, 0));

    var conf = new PrettyConfFactory().apply(Set.of(polygon));
    assertTrue(conf.dpbThreshold() >= 5);
  }

  @Test
  void test_void_input() {
    var conf = new PrettyConfFactory().apply(Set.of());
    assertEquals(10d, conf.dpbThreshold());
  }

  @Test
  void test_null_input() {
    var conf = new PrettyConfFactory().apply(null);
    assertEquals(10d, conf.dpbThreshold());
  }
}
