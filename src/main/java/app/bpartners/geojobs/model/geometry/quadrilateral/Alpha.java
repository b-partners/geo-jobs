package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class Alpha implements Supplier<Set<Quadrilateral>> {

  private final MultiPolygon p;
  private final double minCoverageOfAbstractedArea;
  private final double minAbstractArea;

  public Alpha(Polygon p, double minCoverageOfAbstractedArea, double minAbstractArea) {
    this(
        geometryFactory.createMultiPolygon(new Polygon[] {p}),
        minCoverageOfAbstractedArea,
        minAbstractArea);
  }

  @Override
  public Set<Quadrilateral> get() {
    Set<Quadrilateral> quadrilaterals = new HashSet<>();

    var pMinus = p;
    var unionOf_obbInterP = geometryFactory.createMultiPolygon();
    do {
      var subAplha = new SubAlpha(pMinus);
      quadrilaterals.add(subAplha.get());
      unionOf_obbInterP = union(unionOf_obbInterP, subAplha.obb_inter_p());
      pMinus = diff(p, unionOf_obbInterP, minAbstractArea);
    } while (unionOf_obbInterP.getArea() / p.getArea() < minCoverageOfAbstractedArea);

    return quadrilaterals;
  }

  private static MultiPolygon union(MultiPolygon polygons, Polygon polygon) {
    Geometry newUnion_asGeometry = polygons.union(polygon);
    polygons =
        newUnion_asGeometry instanceof MultiPolygon
            ? (MultiPolygon) newUnion_asGeometry
            : geometryFactory.createMultiPolygon(new Polygon[] {(Polygon) newUnion_asGeometry});
    return polygons;
  }

  private static MultiPolygon diff(Geometry g1, Geometry g2, double minAreaPerPolygon) {
    var nonFilteredDiff = g1.difference(g2);
    Set<Polygon> filtered = new HashSet<>();
    for (int n = 0; n < nonFilteredDiff.getNumGeometries(); n++) {
      var nthGeometry = nonFilteredDiff.getGeometryN(n);
      if (nthGeometry.getArea() > minAreaPerPolygon) {
        filtered.add((Polygon) nthGeometry);
      }
    }
    return geometryFactory.createMultiPolygon(filtered.stream().toArray(Polygon[]::new));
  }
}
