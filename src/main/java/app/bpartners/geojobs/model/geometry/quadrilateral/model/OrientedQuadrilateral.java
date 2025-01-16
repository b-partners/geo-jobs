package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.HaveAnglesSameDirection;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.LineInt;
import app.bpartners.geojobs.model.geometry.route.ContinuationConf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public record OrientedQuadrilateral(
    Quadrilateral quadrilateral, ContinuationOrientation continuationOrientation) {
  public Optional<OrientedQuadrilateral> continueWith(
      OrientedQuadrilateral that, ContinuationConf continuationConf) {
    var origin = geometryFactory.createPoint(new Coordinate(0, 0));
    var distanceThreshold = continuationConf.distanceThreshold();
    if (that.quadrilateral.centroid().distance(origin) < quadrilateral.centroid().distance(origin)
        & isCloseEnoughWith(that, distanceThreshold)) {
      return that.continueWith(this, continuationConf);
    }
    if (!isCloseEnoughWith(that, distanceThreshold)
        || !hasContinuableDirectionWith(that, continuationConf.directionThreshold())) {
      return Optional.empty();
    }

    var continuedQuadrilateral =
        Quadrilateral.fromIntXYCoordinates(continuationCoordinatesFrom(this, that));
    return Optional.of(new OrientedQuadrilateral(continuedQuadrilateral, continuationOrientation));
  }

  private static Set<IntXY> continuationCoordinatesFrom(
      OrientedQuadrilateral oq1, OrientedQuadrilateral oq2) {
    var removableCoordinates1 =
        new HashSet<>(
            Arrays.stream(oq1.quadrilateral.polygon().getCoordinates()).map(IntXY::new).toList());
    var removableCoordinates2 =
        new HashSet<>(
            Arrays.stream(oq2.quadrilateral.polygon().getCoordinates()).map(IntXY::new).toList());

    var shortest1 = shortestLineFrom(removableCoordinates1, removableCoordinates2);
    removableCoordinates1.remove(shortest1.a());
    removableCoordinates2.remove(shortest1.b());
    var shortest2 = shortestLineFrom(removableCoordinates1, removableCoordinates2);

    return Set.of(shortest1.a(), shortest1.b(), shortest2.a(), shortest2.b());
  }

  private static LineInt shortestLineFrom(Set<IntXY> xy1, Set<IntXY> xy2) {
    LineInt res = null;
    var minLength = Double.MAX_VALUE;

    for (var c1 : xy1) {
      for (var c2 : xy2) {
        var length = new LineInt(c1, c2).length();
        if (length < minLength) {
          minLength = length;
          res = new LineInt(c1, c2);
        }
      }
    }
    return res;
  }

  private boolean hasContinuableDirectionWith(
      OrientedQuadrilateral that, double directionThreshold) {
    return switch (continuationOrientation) {
      case lengthOnly ->
          switch (that.continuationOrientation) {
            case lengthOnly, lengthOrWidth ->
                new HaveAnglesSameDirection(directionThreshold)
                    .test(quadrilateral().angle(), that.quadrilateral.angle());
          };
      case lengthOrWidth ->
          switch (that.continuationOrientation) {
            case lengthOnly -> that.hasContinuableDirectionWith(this, directionThreshold);
            case lengthOrWidth -> false;
          };
    };
  }

  private boolean isCloseEnoughWith(OrientedQuadrilateral that, double distanceThreshold) {
    var distance = quadrilateral.polygon().distance(that.quadrilateral.polygon());
    return distance < distanceThreshold;
  }
}
