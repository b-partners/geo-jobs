package app.bpartners.geojobs.model.geometry.quadrilateral;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOnly;

import app.bpartners.geojobs.model.geometry.GeometryDiff;
import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.OrientedQuadrilateral;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
@Slf4j
public class Alpha implements Supplier<Set<OrientedQuadrilateral>> {

  private final MultiPolygon p;
  private final AlphaConf conf;
  private final Set<OrientedQuadrilateral> oqSet;

  public Alpha(Polygon p, AlphaConf conf) {
    var multipolygon = geometryFactory.createMultiPolygon(new Polygon[] {p});
    this.p = multipolygon;
    this.conf = conf;
    oqSet = oqSet(multipolygon, conf);
  }

  private static Set<OrientedQuadrilateral> oqSet(MultiPolygon p, AlphaConf conf) {
    try {
      return fallibleAplha(p, conf);
    } catch (Exception e) {
      log.error(String.format("Alpha failed: polygon=%s", p), e);
      return Set.of();
    }
  }

  @Override
  public Set<OrientedQuadrilateral> get() {
    return oqSet;
  }

  private static Set<OrientedQuadrilateral> fallibleAplha(MultiPolygon p, AlphaConf conf) {
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
          new OrientedQuadrilateral(subAlpha.get(), lengthOnly));
      unionOf_obbInterP = new MultiPolygonUnion().apply(unionOf_obbInterP, subAlpha.obb_inter_p());
      pMinus = new GeometryDiff(conf.minAbstractArea()).apply(p, unionOf_obbInterP);
    } while (unionOf_obbInterP.getArea() / p.getArea() < conf.minCoverageOfAbstractedArea());

    return oqSet;
  }
}
