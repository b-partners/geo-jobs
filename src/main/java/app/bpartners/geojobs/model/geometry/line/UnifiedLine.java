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

  public UnifiedLine(Set<Polygon> toUnify) {
    this.toUnify = toUnify;
    this.unified = unify(toUnify);
  }

  private static Set<Polygon> unify(Set<Polygon> toUnify) {
    var multiPolygon = geometryFactory.createMultiPolygon();
    for (Polygon polygon : toUnify) {
      multiPolygon = new MultiPolygonUnion().apply(multiPolygon, polygon);
    }

    var unified = new HashSet<Polygon>();
    for (int p = 0; p < multiPolygon.getNumGeometries(); p++) {
      unified.add((Polygon) multiPolygon.getGeometryN(p));
    }
    return unified;
  }
}
