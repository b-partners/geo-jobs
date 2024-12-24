package app.bpartners.geojobs.model.geometry.line;

import app.bpartners.geojobs.model.geometry.polygon.MultiPolygonUnion;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha;
import app.bpartners.geojobs.model.geometry.quadrilateral.Alpha.AlphaConf;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;

@AllArgsConstructor
public class LineContinuer implements BiFunction<MultiPolygon, MultiPolygon, MultiPolygon> {

  private final double directionThreshold;
  private final AlphaConf alphaConf;

  @Override
  public MultiPolygon apply(MultiPolygon p1, MultiPolygon p2) {
    var oqList1 = new Alpha(p1, alphaConf).get();
    var oqList2 = new Alpha(p2, alphaConf).get();

    var res = p1;
    for (var oq1 : oqList1) {
      for (var oq2 : oqList2) {
        var continuationOpt = oq1.continueWith(oq2, directionThreshold);
        if (continuationOpt.isPresent()) {
          res = new MultiPolygonUnion().apply(res, continuationOpt.get().quadrilateral().polygon());
        }
      }
    }

    return res;
  }
}
