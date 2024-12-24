package app.bpartners.geojobs.model.geometry.quadrilateral.model;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.model.geometry.HaveAnglesSameDirection;
import java.util.Optional;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;

public record OrientedQuadrilateral(Quadrilateral quadrilateral, Orientation orientation) {
  public Optional<OrientedQuadrilateral> continueWith(
      OrientedQuadrilateral that, double directionThreshold) {
    var origin = geometryFactory.createPoint(new Coordinate(0, 0));
    if (that.quadrilateral.centroid().distance(origin)
        < quadrilateral.centroid().distance(origin)) {
      return that.continueWith(this, directionThreshold);
    }
    if (!closeEnoughWith(that) || !colinearEnoughWith(that, directionThreshold)) {
      return Optional.empty();
    }

    var continuedQuadrilateral =
        new Quadrilateral(
            switch (orientation) {
              case width ->
                  Set.of(
                      quadrilateral.a(), that.quadrilateral.b(),
                      quadrilateral.d(), that.quadrilateral.c());
              case length ->
                  Set.of(
                      quadrilateral.d(), that.quadrilateral.a(),
                      quadrilateral.c(), that.quadrilateral.b());
            });
    return Optional.of(new OrientedQuadrilateral(continuedQuadrilateral, orientation));
  }

  private boolean colinearEnoughWith(OrientedQuadrilateral that, double directionThreshold) {
    return orientation.equals(that.orientation)
        && new HaveAnglesSameDirection(directionThreshold)
            .test(quadrilateral().angle(), that.quadrilateral.angle());
  }

  private boolean closeEnoughWith(OrientedQuadrilateral that) {
    return true;
  }
}
