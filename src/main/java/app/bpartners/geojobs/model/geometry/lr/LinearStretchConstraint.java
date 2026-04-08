package app.bpartners.geojobs.model.geometry.lr;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.math.Vector2D;

public class LinearStretchConstraint {
  private final double tolerance;
  private final Vector2D direction;
  private final Coordinate stretchPoint;
  private final List<Coordinate> startPoints;
  private final List<Coordinate> endPoints;

  public LinearStretchConstraint(
      Coordinate stretchPoint, Coordinate baseNeighbor, double tolerance) {
    this.tolerance = tolerance;
    this.stretchPoint = stretchPoint;
    this.endPoints = new ArrayList<>(List.of(baseNeighbor));
    this.startPoints = new ArrayList<>(List.of(stretchPoint));
    this.direction = new Vector2D(stretchPoint, baseNeighbor).normalize();
  }

  public boolean addToStart(Coordinate candidate) {
    if (isAligned(candidate)) {
      this.startPoints.add(candidate);
      return true;
    }
    return false;
  }

  public boolean addToEnd(Coordinate candidate) {
    if (isAligned(candidate)) {
      this.endPoints.add(candidate);
      return true;
    }
    return false;
  }

  private boolean isAligned(Coordinate candidate) {
    if (stretchPoint == candidate) return true;

    var candidateDirection = new Vector2D(stretchPoint, candidate).normalize();
    var dot = direction.dot(candidateDirection);
    return Math.abs(dot) >= tolerance;
  }

  public Coordinate getStart() {
    return startPoints.getLast();
  }

  public Coordinate getEnd() {
    return endPoints.getLast();
  }
}
