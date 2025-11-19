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

    double p1x = coordinates[0].getX();
    double p1y = coordinates[0].getY();
    double p1z = coordinates[0].getZ();

    double p2x = coordinates[1].getX();
    double p2y = coordinates[1].getY();
    double p2z = coordinates[1].getZ();

    double p3x = coordinates[2].getX();
    double p3y = coordinates[2].getY();
    double p3z = coordinates[2].getZ();

    double a1 = (p2y - p1y) * (p3z - p1z) - (p3y - p1y) * (p2z - p1z);
    double a2 = (p3x - p1x) * (p2z - p1z) - (p2x - p1x) * (p3z - p1z);
    double a3 = (p2x - p1x) * (p3y - p1y) - (p3x - p1x) * (p2y - p1y);

    double norm = Math.sqrt(a1 * a1 + a2 * a2 + a3 * a3);

    double cosnx = a1 / norm;
    double cosny = a2 / norm;
    double cosnz = a3 / norm;

    int n = coordinates.length;
    double s =
        cosnz * (coordinates[n - 1].getX() * p1y - p1x * coordinates[n - 1].getY())
            + cosnx * (coordinates[n - 1].getY() * p1z - p1y * coordinates[n - 1].getZ())
            + cosny * (coordinates[n - 1].getZ() * p1x - p1z * coordinates[n - 1].getX());

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
