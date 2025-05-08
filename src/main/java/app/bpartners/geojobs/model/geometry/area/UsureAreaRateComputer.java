package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import org.locationtech.jts.geom.Polygon;

public class UsureAreaRateComputer extends AreaRateComputer {
  private static final double weight = 0.4;
  private final FeatureMapper featureMapper = new FeatureMapper();
  private final double roofArea;
  private final DetectedTile tile;

  public UsureAreaRateComputer(double roofArea, DetectedTile tile) {
    this.roofArea = roofArea;
    this.tile = tile;
  }

  @Override
  public double compute(DetectableType detectableType) {
    if (roofArea <= 0) {
      throw new BadRequestException(
          "Roof area cannot be zero or negative, current value" + roofArea);
    }

    double computedArea =
        tile.getDetectedObjects().stream()
            .filter(o -> o.getDetectableObjectType().equals(detectableType))
            .map(DetectedObject::getFeature)
            .map(featureMapper::toDomain)
            .mapToDouble(Polygon::getArea)
            .sum();

    return (getMalus(detectableType) * computedArea) / roofArea;
  }

  private int getMalus(DetectableType detectableType) {
    return switch (detectableType) {
      case USURE_LEGER -> 1;
      case USURE_IMPORTANTE -> 2;
      default ->
          throw new NotImplementedException(
              "Detectable type " + detectableType + " malus not implemented");
    };
  }

  public double getUsureAreaRate() {
    return (compute(USURE_LEGER) + compute(USURE_IMPORTANTE)) * 100;
  }

  public double getGlobalRate() {
    return weight * getUsureAreaRate();
  }
}
