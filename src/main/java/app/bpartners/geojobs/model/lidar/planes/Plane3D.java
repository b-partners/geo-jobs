package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.LasPointsDelimiter;
import app.bpartners.geojobs.model.lidar.Polygon3DArea;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import lombok.*;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class Plane3D {
  protected final double a;
  protected final double b;
  protected final double c;
  protected final double d;
  protected final Kernel kernel;
  protected final Set<LasPointGeometry> points;

  protected final double delimitationConcaveRatio;
  protected final double delimitationSimplificationEpsilon;
  protected final Plane3DExtractionStepExporter exporter;

  private Double norm;
  private Polygon3DArea area;
  protected Polygon delimitation;
  private Plane3DSlopeInDegrees slopeInDegrees;
  private Polygon convexDelimitation;

  public static Plane3D fit(
      Kernel kernel,
      double delimitationConcaveRatio,
      double delimitationSimplificationEpsilon,
      Plane3DExtractionStepExporter exporter) {
    var chains = kernel.getChains();
    var triplet = chains.getOrthogonalTriplet();
    var p1 = triplet.getFirst();
    var p2 = triplet.get(1);
    var p3 = triplet.getLast();
    var plane = fromTriplet(p1, p2, p3);

    return plane.toBuilder()
        .kernel(kernel)
        .exporter(exporter)
        .delimitationConcaveRatio(delimitationConcaveRatio)
        .delimitationSimplificationEpsilon(delimitationSimplificationEpsilon)
        .build();
  }

  private static Plane3D fromTriplet(
      LasPointGeometry p1, LasPointGeometry p2, LasPointGeometry p3) {
    var v1 = p2.subtract(p1);
    var v2 = p3.subtract(p1);
    var normal = v1.cross(v2).normalized();
    var d = -normal.dot(p1);
    var a = normal.getX();
    var b = normal.getY();
    var c = normal.getZ();

    return Plane3D.builder()
        .a(normal.getX())
        .b(normal.getY())
        .c(normal.getZ())
        .d(d)
        .norm(Math.sqrt(a * a + b * b + c * c))
        .points(Set.of(p1, p2, p3))
        .build();
  }

  public double distance(LasPointGeometry p) {
    return Math.abs(a * p.getX() + b * p.getY() + c * p.getZ() + d) / getNorm();
  }

  public Plane3D with(Set<LasPointGeometry> points) {
    return this.toBuilder().points(points).build();
  }

  public static Plane3D empty() {
    return Plane3D.builder()
        .a(0)
        .b(0)
        .c(0)
        .d(0)
        .norm(1d)
        .points(Set.of())
        .delimitationConcaveRatio(0)
        .delimitationSimplificationEpsilon(0)
        .exporter(null)
        .build();
  }

  public Plane3DSlopeInDegrees getSlopeInDegrees() {
    if (slopeInDegrees == null) {
      slopeInDegrees = new Plane3DSlopeInDegrees(points);
    }

    return slopeInDegrees;
  }

  public Polygon getDelimitation() {
    if (delimitation == null) {
      var delimiter =
          new LasPointsDelimiter(
              points, delimitationConcaveRatio, delimitationSimplificationEpsilon, exporter);
      delimitation = delimiter.getPolygon();
      delimitation = makePlane(delimitation);
    }

    return delimitation;
  }

  public double get2DArea() {
    return getDelimitation().getArea();
  }

  public double getNorm() {
    if (norm == null) {
      norm = Math.sqrt(a * a + b * b + c * c);
    }
    return norm;
  }

  public double getArea() {
    if (area == null) {
      area = new Polygon3DArea(getDelimitation());
    }

    return area.getValue();
  }

  /* Or simply with average */
  private Polygon makePlane(Polygon polygon) {
    var coordinates = polygon.getCoordinates();
    var projected = new Coordinate[coordinates.length];

    for (int i = 0; i < coordinates.length; i++) {
      double x = coordinates[i].getX();
      double y = coordinates[i].getY();
      double z = coordinates[i].getZ();

      double t = (a * x + b * y + c * z + d) / (a * a + b * b + c * c);

      double xProj = x - a * t;
      double yProj = y - b * t;
      double zProj = z - c * t;

      projected[i] = new Coordinate(xProj, yProj, zProj);
    }

    return geometryFactory.createPolygon(projected);
  }

  public Plane3D merge(Plane3D other) {
    if (other.getPoints().size() > this.getPoints().size()) {
      return other.merge(this);
    }

    var mergedPoints = new HashSet<>(this.points);
    mergedPoints.addAll(other.getPoints());

    return this.toBuilder()
        .area(null)
        .delimitation(null)
        .slopeInDegrees(null)
        .points(mergedPoints)
        .build();
  }

  public Polygon getConvexDelimitation() {
    if (convexDelimitation == null) {
      convexDelimitation = getConvexDelimitation(this);
    }
    return convexDelimitation;
  }

  private static Polygon getConvexDelimitation(Plane3D plane) {
    var coordinates =
        plane.getPoints().stream().map(LasPointGeometry::getCoordinate).toArray(Coordinate[]::new);
    var hull = new ConvexHull(coordinates, geometryFactory).getConvexHull();

    if (hull instanceof Polygon polygon) {
      return polygon;
    }

    throw new IllegalArgumentException("Invalid polygon retrieved from plane");
  }
}
