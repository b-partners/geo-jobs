package app.bpartners.geojobs.service.lidar.model.geometry;

import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@RequiredArgsConstructor
public class Polygon3DArea {
  private Double value;
  private final Polygon polygon;

  public double getValue() {
    if (value == null) {
      value = compute3DPolygonArea(polygon.getCoordinates());
    }

    return value;
  }

  // https://surveytransfer.net/qgis-3d-area-calculation-solving-the-impossible-polygon-surface-calculation/
  // https://github.com/caoguolin/3D-Polygon-area
  public static double compute3DPolygonArea(Coordinate[] coordinates) {
    if (coordinates.length < 3) {
      return 0.0;
    }

    double P1X = coordinates[0].getX(), P1Y = coordinates[0].getY(), P1Z = coordinates[0].getZ();
    double P2X = coordinates[1].getX(), P2Y = coordinates[1].getY(), P2Z = coordinates[1].getZ();
    double P3X = coordinates[2].getX(), P3Y = coordinates[2].getY(), P3Z = coordinates[2].getZ();

    double a1 = (P2Y - P1Y) * (P3Z - P1Z) - (P3Y - P1Y) * (P2Z - P1Z);
    double a2 = (P3X - P1X) * (P2Z - P1Z) - (P2X - P1X) * (P3Z - P1Z);
    double a3 = (P2X - P1X) * (P3Y - P1Y) - (P3X - P1X) * (P2Y - P1Y);

    double norm = Math.sqrt(a1 * a1 + a2 * a2 + a3 * a3);

    double cosnx = a1 / norm;
    double cosny = a2 / norm;
    double cosnz = a3 / norm;

    int n = coordinates.length;
    double s =
        cosnz * (coordinates[n - 1].getX() * P1Y - P1X * coordinates[n - 1].getY())
            + cosnx * (coordinates[n - 1].getY() * P1Z - P1Y * coordinates[n - 1].getZ())
            + cosny * (coordinates[n - 1].getZ() * P1X - P1Z * coordinates[n - 1].getX());

    // -2 because JTS polygons are closed, but this algorithm expects an open polygon
    for (int i = 0; i < n - 2; i++) {
      var p1 = coordinates[i];
      var p2 = coordinates[i + 1];
      double ss =
          cosnz * (p1.getX() * p2.getY() - p2.getX() * p1.getY())
              + cosnx * (p1.getY() * p2.getZ() - p2.getY() * p1.getZ())
              + cosny * (p1.getZ() * p2.getX() - p2.getZ() * p1.getX());
      s += ss;
    }

    s = Math.abs(s / 2.0);
    return s;
  }
}
