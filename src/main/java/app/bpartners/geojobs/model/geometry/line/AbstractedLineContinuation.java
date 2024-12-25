package app.bpartners.geojobs.model.geometry.line;

import java.util.Optional;
import java.util.function.Supplier;
import org.locationtech.jts.geom.Polygon;

public class AbstractedLineContinuation implements Supplier<Optional<Polygon>> {

  private final AbstractedLine l1, l2;
  private final double directionThreshold;
  private final Optional<Polygon> continuationOpt;

  public AbstractedLineContinuation(
      AbstractedLine l1, AbstractedLine l2, double directionThreshold) {
    this.l1 = l1;
    this.l2 = l2;
    this.directionThreshold = directionThreshold;
    this.continuationOpt = continueLine();
  }

  private Optional<Polygon> continueLine() {
    var oqSet1 = l1.abstraction();
    var oqSet2 = l2.abstraction();

    var res = l1.line().union(l2.line());
    for (var oq1 : oqSet1) {
      for (var oq2 : oqSet2) {
        var continuationOpt = oq1.continueWith(oq2, directionThreshold);
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
