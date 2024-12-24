package app.bpartners.geojobs.model.geometry.line;

import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

@AllArgsConstructor
public class LineContinuer implements BiFunction<Polygon, Polygon, Geometry> {

  private final double directionThreshold;
  private final AlphaConf alphaConf;

  @Override
  public Geometry apply(Polygon p1, Polygon p2) {
    var oqList1 = new Alpha(p1, alphaConf).get();
    var oqList2 = new Alpha(p2, alphaConf).get();

    var res = (MultiPolygon) p1.union(p2);
    for (var oq1 : oqList1) {
      for (var oq2 : oqList2) {
        var continuationOpt = oq1.continueWith(oq2, directionThreshold);
        if (continuationOpt.isPresent()) {
          MultiPolygonUnion multiPolygonUnion = new MultiPolygonUnion();
          res = multiPolygonUnion.apply(res, continuationOpt.get().quadrilateral().polygon());
        }
      }
    }

    return res
        // Return a Polygon instead a MultiPolygon whenever possible.
        // Don't know yet if it's a good idea. Have to look at perfs.
        .union();
  }
}
