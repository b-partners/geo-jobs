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
      case PISCINE -> {
        return DetectableType.PISCINE;
      }
      case TOITURE_REVETEMENT -> {
        return DetectableType.TOITURE_REVETEMENT;
      }
      case ARBRE -> {
        return DetectableType.ARBRE;
      }
      case PASSAGE_PIETON -> {
        return DetectableType.PASSAGE_PIETON;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return DetectableType.PANNEAU_PHOTOVOLTAIQUE;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + rest);
    }
  }

  public DetectableObjectType toRest(DetectableType domain) {
    switch (domain) {
      case PISCINE -> {
        return PISCINE;
      }
      case TOITURE_REVETEMENT -> {
        return TOITURE_REVETEMENT;
      }
      case ARBRE -> {
        return ARBRE;
      }
      case PASSAGE_PIETON -> {
        return PASSAGE_PIETON;
      }
      case PANNEAU_PHOTOVOLTAIQUE -> {
        return PANNEAU_PHOTOVOLTAIQUE;
      }
      case TROTTOIR -> {
        return TROTTOIR;
      }
      case LINE -> {
        return LINE;
      }
      case ESPACE_VERT -> {
        return ESPACE_VERT;
      }
      default -> throw new NotImplementedException("Unknown detectable object type " + domain);
    }
  }
}
