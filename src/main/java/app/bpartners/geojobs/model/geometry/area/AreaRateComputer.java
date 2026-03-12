package app.bpartners.geojobs.model.geometry.area;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.Collection;
import org.locationtech.jts.geom.Polygon;

public abstract class AreaRateComputer {
  private final FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(null, null));

  protected double compute(DetectableType detectableType) {
    if (getRoofArea() <= 0) {
      throw new BadRequestException(
          "Roof area cannot be zero or negative, current value" + getRoofArea());
    }
    if (getTile() == null && getPolygonObjectTypes() != null) {
      double computedArea =
          getPolygonObjectTypes().stream()
              .filter(o -> detectableType.equals(o.objectType()))
              .map(PolygonObjectType::polygon)
              .mapToDouble(Polygon::getArea)
              .sum();
      return (getMalus(detectableType) * computedArea) / getRoofArea();
    } else if (getTile() != null) {
      double computedArea =
          getTile().getDetectedObjects().stream()
              .filter(o -> o.getDetectableObjectType().equals(detectableType))
              .map(DetectedObject::getFeature)
              .map(featureMapper::toDomainPolygon)
              .mapToDouble(Polygon::getArea)
              .sum();
      return (getMalus(detectableType) * computedArea) / getRoofArea();
    }
    throw new IllegalStateException(
        "Both tile and polygonObjectTypes can not be null to compute HumiditeAreaRate");
  }

  abstract DetectedTile getTile();

  abstract Collection<PolygonObjectType> getPolygonObjectTypes();

  abstract double getRoofArea();

  abstract int getMalus(DetectableType detectableType);

  abstract double getGlobalRate();
}
