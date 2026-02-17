package app.bpartners.geojobs.model.lidar.planes;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.algorithm.PointsDelimitationComputer.getConvex;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import app.bpartners.geojobs.model.lidar.LasPointsDelimiter;
import app.bpartners.geojobs.model.lidar.Polygon3DArea;
import app.bpartners.geojobs.model.lidar.planes.exporter.Plane3DExtractionStepExporter;
import java.util.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
@Getter
@Builder(toBuilder = true)
@RequiredArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Plane3D {
  protected @EqualsAndHashCode.Include final double a;
  protected @EqualsAndHashCode.Include final double b;
  protected @EqualsAndHashCode.Include final double c;
  protected @EqualsAndHashCode.Include final double d;
  protected @EqualsAndHashCode.Exclude final Kernel kernel;
  protected @EqualsAndHashCode.Exclude final Set<LasPointGeometry> points;

  protected @EqualsAndHashCode.Exclude final double delimitationConcaveRatio;
  protected @EqualsAndHashCode.Exclude final double delimitationSimplificationEpsilon;
  protected @EqualsAndHashCode.Exclude final Plane3DExtractionStepExporter exporter;

  private @EqualsAndHashCode.Include Double norm;
  private @EqualsAndHashCode.Exclude Polygon3DArea area;
  protected @EqualsAndHashCode.Exclude Polygon delimitation;
  protected @EqualsAndHashCode.Exclude Polygon convexDelimitation;
  private @EqualsAndHashCode.Exclude Plane3DSlopeInDegrees slopeInDegrees;

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

    if (normal.getZ() > 0) {
      normal = normal.negate();
    }

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
    return this.toBuilder().points(new HashSet<>(points)).build();
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
      delimitation = projectPolygonToPlane(delimitation);
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
  private Polygon projectPolygonToPlane(Polygon polygon) {
    var coordinates =
        Arrays.stream(polygon.getCoordinates())
            .map(
                coordinate ->
                    new Coordinate(
                        coordinate.getX(),
                        coordinate.getY(),
                        zAt(coordinate.getX(), coordinate.getY())))
            .toArray(Coordinate[]::new);
    return geometryFactory.createPolygon(coordinates);
  }

  public Polygon getConvexDelimitation() {
    if (convexDelimitation == null) {
      var convex = getConvex(points);
      convexDelimitation = projectPolygonToPlane(convex);
    }
    return convexDelimitation;
  }

  public boolean isVertical() {
    return Math.abs(c) < 1e-12;
  }

  public double zAt(double x, double y) {
    if (isVertical()) {
      throw new IllegalStateException("Plane is vertical: z cannot be computed for given x,y");
    }
    return -(a * x + b * y + d) / c;
  }
}
