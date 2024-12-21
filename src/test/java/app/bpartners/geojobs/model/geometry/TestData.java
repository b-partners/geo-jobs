package app.bpartners.geojobs.model.geometry;

import static org.locationtech.jts.geom.util.AffineTransformation.rotationInstance;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class TestData {

  private static final GeometryFactory geometryFactory = new GeometryFactory();

  public static Polygon longPolygon() {
    var polygon =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(40, 40),
                  new Coordinate(60, 60),
                  new Coordinate(100, 30),
                  new Coordinate(200, 400),
                  new Coordinate(200, 500),
                  new Coordinate(40, 40) // Close the ring
                }));
    return polygon;
  }

  public static Polygon croissant1Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(140, 300),
                  new Coordinate(120, 200),
                  new Coordinate(150, 110),
                  new Coordinate(360, 120),
                  new Coordinate(450, 140),
                  new Coordinate(350, 350),
                  new Coordinate(300, 350),
                  new Coordinate(380, 190),
                  new Coordinate(190, 190),
                  new Coordinate(140, 300)
                }));
    var rotation = rotationInstance(Math.PI / 4, 200, 200);

    return (Polygon) rotation.transform(unrotated);
  }

  public static Polygon croissant2Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(140, 300),
                  new Coordinate(120, 200),
                  new Coordinate(150, 110),
                  new Coordinate(360, 120),
                  new Coordinate(450, 140),
                  new Coordinate(350, 350),
                  new Coordinate(300, 350),
                  new Coordinate(380, 190),
                  new Coordinate(160, 160),
                  new Coordinate(140, 300)
                }));
    var rotation = rotationInstance(Math.PI / 4, 200, 200);

    return (Polygon) rotation.transform(unrotated);
  }

  public static Polygon compass1Polygon() {
    var unrotated =
        geometryFactory.createPolygon(
            geometryFactory.createLinearRing(
                new Coordinate[] {
                  new Coordinate(30, 250),
                  new Coordinate(40, 150),
                  new Coordinate(70, 150),
                  new Coordinate(400, 10),
                  new Coordinate(550, 25),
                  new Coordinate(70, 180),
                  new Coordinate(70, 260),
                  new Coordinate(650, 300),
                  new Coordinate(550, 350),
                  new Coordinate(60, 320),
                  new Coordinate(50, 300),
                  new Coordinate(30, 250)
                }));
    var rotation = rotationInstance(Math.PI / 8, 0, 500);

    return (Polygon) rotation.transform(unrotated);
  }
}
