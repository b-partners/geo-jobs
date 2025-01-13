package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.Orientation.length;

import app.bpartners.geojobs.model.geometry.GeometryDiff;
import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Slf4j
@AllArgsConstructor
public class Alpha implements Supplier<Set<OrientedQuadrilateral>> {

  private final MultiPolygon p;
  private final AlphaConf conf;

  public record AlphaConf(double minCoverageOfAbstractedArea, double minAbstractArea) {}

  public Alpha(Polygon p, AlphaConf conf) {
    this(geometryFactory.createMultiPolygon(new Polygon[] {p}), conf);
  }

  @Override
  public Set<OrientedQuadrilateral> get() {
    try {
      return fallibleAplha();
    } catch (Exception e) {
      log.error(String.format("Alpha failed: p=%s", p), e);
      return Set.of();
    }
  }

  private Set<OrientedQuadrilateral> fallibleAplha() {
    Set<OrientedQuadrilateral> oqSet = new HashSet<>();

    var pMinus = p;
    var unionOf_obbInterP = geometryFactory.createMultiPolygon();
    do {
      var subAlpha = new SubAlpha(pMinus);
      oqSet.add(
          // For now, all orientations are on length
          // But later on, considering permitting {length, width} to allow
          // continuation both on length and width for small enough,
          // not reliable enough quadrilateral
          new OrientedQuadrilateral(subAlpha.get(), length));
      unionOf_obbInterP = new MultiPolygonUnion().apply(unionOf_obbInterP, subAlpha.obb_inter_p());
      pMinus = new GeometryDiff(conf.minAbstractArea).apply(p, unionOf_obbInterP);
    } while (unionOf_obbInterP.getArea() / p.getArea() < conf.minCoverageOfAbstractedArea);

    return oqSet;
  }
}
