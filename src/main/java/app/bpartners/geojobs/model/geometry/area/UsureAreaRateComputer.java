package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;

import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.Collection;
import lombok.Getter;

@Getter
public class UsureAreaRateComputer extends AreaRateComputer {
  private static final double weight = 0.4;
  private final double roofArea;
  private final DetectedTile tile;
  private final Collection<PolygonObjectType> polygonObjectTypes;

  public UsureAreaRateComputer(double roofArea, DetectedTile tile) {
    this.roofArea = roofArea;
    this.tile = tile;
    this.polygonObjectTypes = null;
  }

  public UsureAreaRateComputer(double roofArea, Collection<PolygonObjectType> polygonObjectTypes) {
    this.roofArea = roofArea;
    this.polygonObjectTypes = polygonObjectTypes;
    this.tile = null;
  }

  @Override
  int getMalus(DetectableType detectableType) {
    return switch (detectableType) {
      case USURE_LEGER -> 1;
      case USURE_IMPORTANTE -> 2;
      default ->
          throw new NotImplementedException(
              "Detectable type " + detectableType + " malus not implemented");
    };
  }

  public double getUsureAreaRate() {
    var computedAreaRate = (compute(USURE_LEGER) + compute(USURE_IMPORTANTE)) * 100;
    return Math.min(computedAreaRate, 100.0);
  }

  public double getGlobalRate() {
    return WEIGHT * getUsureAreaRate();
  }
}
