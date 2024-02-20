package app.bpartners.geojobs.endpoint.rest.controller.mapper;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import org.springframework.stereotype.Component;

@Component
public class DetectableObjectTypeMapper {
  public DetectableType toDomain(DetectableObjectType rest) {
    switch (rest) {
      case POOL -> {
        return DetectableType.POOL;
      }
      case ROOF -> {
        return DetectableType.ROOF;
      }
      case TREE -> {
        return DetectableType.TREE;
      }
      case PATHWAY -> {
        return DetectableType.PATHWAY;
      }
      case SOLAR_PANEL -> {
        return DetectableType.SOLAR_PANEL;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    }
  }

  public DetectableObjectType toRest(DetectableType domain) {
    switch (domain) {
      case POOL -> {
        return POOL;
      }
      case ROOF -> {
        return ROOF;
      }
      case TREE -> {
        return TREE;
      }
      case PATHWAY -> {
        return PATHWAY;
      }
      case SOLAR_PANEL -> {
        return SOLAR_PANEL;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + domain);
    }
  }
}
