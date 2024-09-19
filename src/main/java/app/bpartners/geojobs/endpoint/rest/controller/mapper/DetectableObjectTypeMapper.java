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
      case TOITURE_REVETEMENT -> {
        return DetectableType.TOITURE_REVETEMENT;
      }
      case TREE -> {
        return DetectableType.TREE;
      }
      case PATHWAY -> {
        return DetectableType.PATHWAY;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return DetectableType.PANNEAU_PHOTOVOLTAIQUE;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    }
  }

  public DetectableObjectType toRest(DetectableType domain) {
    switch (domain) {
      case POOL -> {
        return POOL;
      }
      case TOITURE_REVETEMENT -> {
        return TOITURE_REVETEMENT;
      }
      case TREE -> {
        return TREE;
      }
      case PATHWAY -> {
        return PATHWAY;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return PANNEAU_PHOTOVOLTAIQUE;
      }
      case SIDEWALK -> {
        return SIDEWALK;
      }
      case LINE -> {
        return LINE;
      }
      case GREEN_SPACE -> {
        return GREEN_SPACE;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + domain);
    }
  }
}
