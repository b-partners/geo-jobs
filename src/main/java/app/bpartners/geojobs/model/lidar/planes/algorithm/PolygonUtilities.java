package app.bpartners.geojobs.model.lidar.planes.algorithm;

import org.locationtech.jts.geom.Polygon;

public class PolygonUtilities {
  public static boolean isCompact(Polygon polygon, double epsilon) {
    double area = polygon.getArea();
    double perimeter = polygon.getLength();
    double compactness = 4 * Math.PI * area / (perimeter * perimeter);

    return compactness > epsilon;
  }
}
