package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class ContinuationConfFactoryTest {
  private final GeometryFactory geomFact = new GeometryFactory();

  private LatLonPolygon createRectangle(double x, double y, double width, double height) {
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(x, y),
          new Coordinate(x + width, y),
          new Coordinate(x + width, y + height),
          new Coordinate(x, y + height),
          new Coordinate(x, y)
        };
    return new LatLonPolygon(geomFact.createPolygon(coords));
  }

  @Test
  void testStraightShortPolygons() {
    var poly = createRectangle(0, 0, 10, 2);
    var conf = new ContinuationConfFactory().apply(Set.of(poly));

    assertTrue(conf.distanceThreshold() > 0 && conf.distanceThreshold() < 50);
    assertTrue(conf.minDirectionThreshold() < Math.PI / 12);
    assertTrue(conf.maxDirectionThreshold() <= Math.PI / 2);
    assertTrue(conf.maxDirectionThreshold() > conf.minDirectionThreshold());
  }

  @Test
  void testCurvedLongPolygons() {
    var poly1 = createRectangle(0, 0, 100, 10);
    var poly2 = createRectangle(110, 10, 100, 10);
    var conf = new ContinuationConfFactory().apply(Set.of(poly1, poly2));

    assertTrue(conf.distanceThreshold() > 100);
    assertTrue(conf.minDirectionThreshold() > Math.PI / 36);
    assertTrue(conf.maxDirectionThreshold() <= Math.PI / 2);
  }

  @Test
  void testMixedPolygons() {
    var small = createRectangle(0, 0, 10, 2);
    var large = createRectangle(100, 0, 200, 20);
    var conf = new ContinuationConfFactory().apply(Set.of(small, large));

    assertTrue(conf.distanceThreshold() > 10);
    assertTrue(conf.minDirectionThreshold() > 0);
    assertTrue(conf.maxDirectionThreshold() > conf.minDirectionThreshold());
  }

  @Test
  void testEmptyListFallback() {
    var conf = new ContinuationConfFactory().apply(Set.of());

    assertEquals(50 * 2.5, conf.distanceThreshold());
    assertEquals(Math.PI / 36, conf.minDirectionThreshold());
    assertTrue(conf.maxDirectionThreshold() <= Math.PI / 2);
  }

  @Test
  void testMinMaxRelationship() {
    var poly = createRectangle(0, 0, 30, 30);
    var conf = new ContinuationConfFactory().apply(Set.of(poly));

    assertTrue(conf.maxDirectionThreshold() >= conf.minDirectionThreshold());
    assertTrue(conf.maxDirectionThreshold() <= Math.PI / 2);
  }
}
