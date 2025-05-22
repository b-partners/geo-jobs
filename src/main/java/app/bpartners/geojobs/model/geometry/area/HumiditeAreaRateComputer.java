package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_CLAIR;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE_INTENSE;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import org.locationtech.jts.geom.Polygon;

public class HumiditeAreaRateComputer extends AreaRateComputer {
  private static final double weight = 0.3;
  private final FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(null));
  private final double roofArea;
  private final DetectedTile tile;

  public HumiditeAreaRateComputer(double roofArea, DetectedTile tile) {
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
      case HUMIDITE_CLAIR -> 1;
      case HUMIDITE_INTENSE -> 2;
      default ->
          throw new NotImplementedException(
              "Detectable type " + detectableType + " malus not implemented");
    };
  }

  public double getHumidityAreaRate() {
    return (compute(HUMIDITE_CLAIR) + compute(HUMIDITE_INTENSE)) * 100;
  }

  @Override
  public double getGlobalRate() {
    return weight * getHumidityAreaRate();
  }
}
