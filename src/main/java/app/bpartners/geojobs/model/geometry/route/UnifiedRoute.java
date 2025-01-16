package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class UnifiedRoute {
  private final Set<Polygon> toUnify, unified;
  private final UnionConf unionConf;

  public UnifiedRoute(Set<Polygon> toUnify, UnionConf unionConf) {
    this.unionConf = unionConf;
    this.toUnify = toUnify;
    this.unified = unify(toUnify, unionConf);
  }

  private static Set<Polygon> unify(Set<Polygon> toUnify, UnionConf unionConf) {
    var multiPolygon = geometryFactory.createMultiPolygon();
    for (Polygon polygon : toUnify) {
      multiPolygon =
          new MultiPolygonUnion().apply(multiPolygon, (Polygon) polygon.buffer(unionConf.buffer()));
    }

    var unified = new HashSet<Polygon>();
    for (int p = 0; p < multiPolygon.getNumGeometries(); p++) {
      unified.add((Polygon) multiPolygon.getGeometryN(p));
    }
    return unified;
  }
}
