package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOnly;
import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOrWidth;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Accessors(fluent = true)
@Getter
public enum RouteType {
  road(lengthOnly),
  pathway(lengthOrWidth),
  sidewalk(lengthOnly);

  private final ContinuationOrientation continuationOrientation;

  public static RouteType routeTypeFrom(DetectableObjectType detectableType) {
    return switch (detectableType) {
      case PASSAGE_PIETON -> pathway;
      case LINE -> road;
      case TROTTOIR -> sidewalk;
      case RISQUE_FEU,
              TOITURE_REVETEMENT,
              ESPACE_VERT_PARKING,
              PANNEAU_PHOTOVOLTAIQUE,
              PISCINE,
              ARBRE,
              ESPACE_VERT,
              VOIE_CARROSSABLE,
              PARKING,
              MOISISSURE_CLAIR,
              MOISISSURE_COULEUR,
              MOISISSURE_NOIRCIE,
              USURE_IMPORTANTE,
              USURE_LEGER,
              FISSURE_CASSURE,
              OBSTACLE,
              CHEMINEE,
              HUMIDITE_CLAIR,
              HUMIDITE_INTENSE,
              VELUX,
              BATI_TUILES,
              BATI_BETON,
              BATI_ARDOISE,
              BATI_AUTRES ->
          throw new IllegalArgumentException("Unsupported continuation on " + detectableType);
    };
  }

  public static RouteType routeTypeFrom(String label) {
    return switch (label.toLowerCase()) {
      case "pathway", "passage_pieton" -> pathway;
      case "line" -> road;
      case "sidewalk" -> sidewalk;
      default -> throw new IllegalArgumentException("Unsupported continuation on " + label);
    };
  }
}
