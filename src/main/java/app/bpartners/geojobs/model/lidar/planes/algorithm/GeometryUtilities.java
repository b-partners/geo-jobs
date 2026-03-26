package app.bpartners.geojobs.model.lidar.planes.algorithm;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

public class GeometryUtilities {
  private GeometryUtilities() {}

  public static boolean isCompact(Polygon polygon, double epsilon) {
    double area = polygon.getArea();
    double perimeter = polygon.getLength();
    double compactness = 4 * Math.PI * area / (perimeter * perimeter);

    return compactness > epsilon;
  }

  public static Geometry intersection(Geometry a, Geometry b) {
    if (!a.isValid()) a = a.buffer(0);
    if (!b.isValid()) b = b.buffer(0);
    return a.intersection(b);
  }

  public static LasPointGeometry centroid(Collection<LasPointGeometry> points) {
    double sx = 0;
    double sy = 0;
    double sz = 0;
    for (var point : points) {
      sx += point.getX();
      sy += point.getY();
      sz += point.getZ();
    }

    int count = points.size();
    return new LasPointGeometry(sx / count, sy / count, sz / count);
  }

  public static Coordinate[] zSetter(Coordinate[] coordinates, ZSetterCallback callback) {
    return Arrays.stream(coordinates)
        .map(
            coordinate -> {
              var x = coordinate.getX();
              var y = coordinate.getY();
              var z = callback.apply(coordinate);
              return new Coordinate(x, y, z);
            })
        .toArray(Coordinate[]::new);
  }

  public static Polygon zSetter(Polygon polygon, ZSetterCallback callback) {
    var projectedCoordinates = zSetter(polygon.getCoordinates(), callback);
    return geometryFactory.createPolygon(projectedCoordinates);
  }

  public static Polygon project(Plane3D plane, Polygon polygon) {
    return zSetter(polygon, coordinate -> plane.zAt(coordinate.getX(), coordinate.getY()));
  }

  public interface ZSetterCallback extends Function<Coordinate, Double> {}

  public static Polygon getLargestPolygon(Geometry geometry) {
    if (geometry instanceof Polygon polygon) {
      return polygon;
    }

    if (geometry instanceof MultiPolygon multiPolygon) {
      Polygon largest = null;
      double maxArea = -1.0;

      for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
        var current = (Polygon) multiPolygon.getGeometryN(i);
        double currentArea = current.getArea();

        if (currentArea > maxArea) {
          maxArea = currentArea;
          largest = current;
        }
      }
      return largest;
    }

    throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
  }
}
