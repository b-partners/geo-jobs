package app.bpartners.geojobs.model.geometry.route;

import java.util.Optional;
import java.util.function.Supplier;
import org.locationtech.jts.geom.Polygon;

public class AbstractRouteContinuation implements Supplier<Optional<Polygon>> {

  private final AbstractRoute l1, l2;
  private final ContinuationConf continuationConf;
  private final Optional<Polygon> continuationOpt;

  public AbstractRouteContinuation(
      AbstractRoute l1, AbstractRoute l2, ContinuationConf continuationConf) {
    this.l1 = l1;
    this.l2 = l2;
    this.continuationConf = continuationConf;
    this.continuationOpt = continueLine();
  }

  private Optional<Polygon> continueLine() {
    var oqSet1 = l1.abstraction();
    var oqSet2 = l2.abstraction();

    var res = l1.line().union(l2.line());
    for (var oq1 : oqSet1) {
      for (var oq2 : oqSet2) {
        var continuationOpt = oq1.continueWith(oq2, continuationConf);
        if (continuationOpt.isPresent()) {
          res = res.union(continuationOpt.get().quadrilateral().polygon());
        }
      }
    }

    return res instanceof Polygon ? Optional.of((Polygon) res) : Optional.empty();
  }

  @Override
  public Optional<Polygon> get() {
    return continuationOpt;
  }
}
