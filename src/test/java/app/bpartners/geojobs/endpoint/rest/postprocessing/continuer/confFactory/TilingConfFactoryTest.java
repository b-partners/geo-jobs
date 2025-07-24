package app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.confFactory;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

class TilingConfFactoryTest {

  private static final GeometryFactory factory = new GeometryFactory();

  private LatLonPolygon createPolygon(double size) {
    Coordinate[] coords =
        new Coordinate[] {
          new Coordinate(0, 0),
          new Coordinate(size, 0),
          new Coordinate(size, size),
          new Coordinate(0, size),
          new Coordinate(0, 0)
        };
    LinearRing ring = factory.createLinearRing(coords);
    Polygon polygon = factory.createPolygon(ring, null);
    return new LatLonPolygon(polygon);
  }

  @Test
  public void testEmptyPolygonsReturnsDefault() {
    TilingConf conf = new TilingConfFactory().apply(Set.of());
    assertEquals(20, conf.z());
    assertEquals(1_024, conf.imgSize());
  }

  @Test
  public void testNullInputReturnsDefault() {
    TilingConf conf = new TilingConfFactory().apply(null);
    assertEquals(20, conf.z());
    assertEquals(1_024, conf.imgSize());
  }

  @Test
  public void testLargePolygonLowZoom() {
    LatLonPolygon poly = createPolygon(180);
    TilingConf conf = new TilingConfFactory().apply(Set.of(poly));
    assertTrue(conf.z() >= 0 && conf.z() <= 5, "Zoom level for large polygon should be low");
    assertEquals(1_024, conf.imgSize());
  }

  @Test
  public void testSmallPolygonHighZoom() {
    LatLonPolygon poly = createPolygon(0.0001);
    TilingConf conf = new TilingConfFactory().apply(Set.of(poly));
    assertTrue(conf.z() >= 15 && conf.z() <= 20, "Zoom level for small polygon should be high");
    assertEquals(1_024, conf.imgSize());
  }

  @Test
  public void testMixedSizePolygonsChooseAppropriateZoom() {
    LatLonPolygon big = createPolygon(100);
    LatLonPolygon small = createPolygon(0.001d);
    TilingConf conf = new TilingConfFactory().apply(Set.of(big, small));

    assertTrue(conf.z() >= 0 && conf.z() <= 10);
  }
}
