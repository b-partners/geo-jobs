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
  private static final int MIN_BUFFER_REQUIRED = 20;
  private final Set<Polygon> toUnify, unified;

  public UnifiedLine(Set<Polygon> toUnify) {
    this.toUnify = toUnify;
    this.unified = unify(toUnify);
  }

  private static Set<Polygon> unify(Set<Polygon> toUnify) {
    var multiPolygon = geometryFactory.createMultiPolygon();
    for (Polygon polygon : toUnify) {
      multiPolygon =
          new MultiPolygonUnion()
              .apply(multiPolygon, (Polygon) polygon.buffer(MIN_BUFFER_REQUIRED));
    }

    var unified = new HashSet<Polygon>();
    for (int p = 0; p < multiPolygon.getNumGeometries(); p++) {
      unified.add((Polygon) multiPolygon.getGeometryN(p));
    }
    return unified;
  }
}
