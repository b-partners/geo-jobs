package app.bpartners.geojobs.model.geometry.area;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;

import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.Collection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class MoisissureAreaRateComputer extends AreaRateComputer {
  static final double WEIGHT = 0.8;
  private final FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(), null);
  private final double roofArea;
  private final DetectedTile tile;
  private final Collection<PolygonObjectType> polygonObjectTypes;

  public MoisissureAreaRateComputer(double roofArea, DetectedTile tile) {
    this.roofArea = roofArea;
    this.tile = tile;
    this.polygonObjectTypes = null;
  }

  public MoisissureAreaRateComputer(
      double roofArea, Collection<PolygonObjectType> polygonObjectTypes) {
    this.roofArea = roofArea;
    this.polygonObjectTypes = polygonObjectTypes;
    this.tile = null;
  }

  public double getMoisissureAreaRate() {
    var computedAreaRate =
        (compute(MOISISSURE_NOIRCIE) + compute(MOISISSURE_CLAIR) + compute(MOISISSURE_COULEUR))
            * 100;
    return Math.min(computedAreaRate, 100.0);
  }

  @Override
  int getMalus(DetectableType detectableType) {
    return 1; // no malus for moisissure
  }

  @Override
  public double getGlobalRate() {
    return WEIGHT * getMoisissureAreaRate();
  }
}
