package app.bpartners.geojobs.model.geometry.line;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.locationtech.jts.geom.Polygon;

@Accessors(fluent = true)
@Getter
public class UnifiedLine {
  private final Set<Polygon> toUnify, unified;
  private final UnificationConf unificationConf;

  public UnifiedLine(Set<Polygon> toUnify, UnificationConf unificationConf) {
    this.unificationConf = unificationConf;
    this.toUnify = toUnify;
    this.unified = unify(toUnify, unificationConf);
  }

  private static Set<Polygon> unify(Set<Polygon> toUnify, UnificationConf unificationConf) {
    var multiPolygon = geometryFactory.createMultiPolygon();
    for (Polygon polygon : toUnify) {
      multiPolygon =
          new MultiPolygonUnion()
              .apply(multiPolygon, (Polygon) polygon.buffer(unificationConf.buffer()));
    }

    var unified = new HashSet<Polygon>();
    for (int p = 0; p < multiPolygon.getNumGeometries(); p++) {
      unified.add((Polygon) multiPolygon.getGeometryN(p));
    }
    return unified;
  }
}
