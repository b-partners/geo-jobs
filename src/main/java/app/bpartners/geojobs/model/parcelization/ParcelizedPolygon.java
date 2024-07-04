package app.bpartners.geojobs.model.parcelization;

import static java.lang.Math.pow;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.ArcgisRasterZoom;
import app.bpartners.geojobs.model.parcelization.area.IsAreaOfParcel;
import app.bpartners.geojobs.model.parcelization.area.SquareDegree;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class ParcelizedPolygon {
  @Getter private final Polygon polygon;
  @Getter private final ArcgisRasterZoom arcgisRasterZoom;
  @Getter private final Set<Polygon> parcels;

  private final IsAreaOfParcel isAreaOfParcel;

  public ParcelizedPolygon(Polygon polygon, ArcgisRasterZoom arcgisRasterZoom) {
    this(polygon, arcgisRasterZoom, new ArcgisRasterZoom(20), new SquareDegree(2 * pow(10, -4)));
  }

  public ParcelizedPolygon(
      Polygon polygon,
      ArcgisRasterZoom targetZoom,
      ArcgisRasterZoom referenceZoom,
      SquareDegree maxParcelAreaAtReferenceZoom) {
    this.polygon = polygon;
    this.arcgisRasterZoom = targetZoom;
    this.isAreaOfParcel = new IsAreaOfParcel(referenceZoom, maxParcelAreaAtReferenceZoom);
    this.parcels = parcelize(polygon);
  }

  private Set<Polygon> parcelize(Polygon thatPolygon) {
    var intersection = thatPolygon.intersection(this.polygon);
    if (isAreaOfParcel.test(intersection, arcgisRasterZoom)) {
      return Set.of((Polygon) intersection);
    }

    var leftHalfEnvelope =
        leftHalfEnvelope(thatPolygon); // envelope = the smallest containing rectangle
    var rightHalfEnvelope = rightHalfEnvelope(thatPolygon);
    return Stream.concat(
            parcelize(leftHalfEnvelope).stream(), parcelize(rightHalfEnvelope).stream())
        .collect(toSet());
  }

  private Polygon leftHalfEnvelope(Polygon polygon) {
    var edges = new NamedEdgesQuadrilateral(envelopeEdgesCoordinates(polygon));
    var a = edges.getA();
    var b = edges.getB();
    var centroidX = polygon.getCentroid().getX();
    return quadrilateralFromSameSideEdgesAndCentroidX(a, b, centroidX);
  }

  private Polygon rightHalfEnvelope(Polygon thatPolygon) {
    var edges = new NamedEdgesQuadrilateral(envelopeEdgesCoordinates(thatPolygon));
    var c = edges.getC();
    var d = edges.getD();
    var centroidX = thatPolygon.getCentroid().getX();
    return quadrilateralFromSameSideEdgesAndCentroidX(c, d, centroidX);
  }

  private static Polygon quadrilateralFromSameSideEdgesAndCentroidX(
      Coordinate edge1, Coordinate edge2, double centroidX) {
    var geometryFactory = new GeometryFactory();
    return geometryFactory.createPolygon(
        new Coordinate[] {
          edge1,
          edge2,
          new Coordinate(centroidX, edge2.y),
          new Coordinate(centroidX, edge1.y),
          edge1 // note(LinearRing): else IllegalArgumentException: Points of LinearRing do not form
          // a closed linestring
        });
  }

  private Set<Coordinate> envelopeEdgesCoordinates(Polygon thatPolygon) {
    return Arrays.stream(thatPolygon.getEnvelope().getCoordinates()).collect(toSet());
  }
}
