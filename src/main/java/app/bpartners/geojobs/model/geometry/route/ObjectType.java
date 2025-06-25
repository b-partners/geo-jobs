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
public enum ObjectType {
  road(lengthOnly),
  pathway(lengthOrWidth),
  line(lengthOnly),
  sidewalk(lengthOnly),
  passage_pieton(lengthOrWidth),
  trottoir(lengthOnly),
  tombe(lengthOrWidth),
  arbre(lengthOrWidth),
  bati_tuiles(lengthOrWidth),
  bati_beton(lengthOrWidth),
  bati_ardoise(lengthOrWidth),
  bati_autres(lengthOrWidth),
  toiture_revetement(lengthOrWidth),
  espace_vert(lengthOrWidth),
  espace_vert_parking(lengthOrWidth),
  piscine(lengthOrWidth),
  panneau_photovoltaique(lengthOrWidth),
  voie_carrossable(lengthOrWidth),
  parking(lengthOrWidth),
  place_standard(lengthOrWidth),
  risque_feu(lengthOrWidth),
  moisissure(lengthOrWidth),
  usure(lengthOrWidth),
  humidite(lengthOrWidth),
  moisissure_clair(lengthOrWidth),
  moisissure_couleur(lengthOrWidth),
  moisissure_noircie(lengthOrWidth),
  usure_importante(lengthOrWidth),
  usure_leger(lengthOrWidth),
  fissure_cassure(lengthOrWidth),
  obstacle(lengthOrWidth),
  cheminee(lengthOrWidth),
  humidite_clair(lengthOrWidth),
  humidite_intense(lengthOrWidth),
  velux(lengthOrWidth),
  background(lengthOrWidth);

  private final ContinuationOrientation continuationOrientation;

  public static ObjectType routeTypeFrom(DetectableObjectType detectableType) {
    return switch (detectableType) {
      case PASSAGE_PIETON -> passage_pieton;
      case LINE -> line;
      case TROTTOIR -> trottoir;
      case RISQUE_FEU,
              MOISISSURE,
              USURE,
              HUMIDITE,
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
              BATI_AUTRES,
              BACKGROUND ->
          throw new IllegalArgumentException("Unsupported continuation on " + detectableType);
    };
  }

  public static ObjectType routeTypeFrom(String label) {
    return switch (label.toLowerCase()) {
      case "pathway", "passage_pieton" -> pathway;
      case "tombe", "tomb" -> tombe;
      case "tree", "arbre" -> arbre;
      case "bati", "bati_tuiles", "bati_beton", "bati_ardoise", "bati_autres" -> bati_autres;
      case "green_space", "espace_vert", "espace_vert_parking" -> espace_vert;
      case "pool", "piscine" -> piscine;
      case "place_standard" -> place_standard;
      case "moisissure" -> moisissure;
      case "moisissure_clair" -> moisissure_clair;
      case "moisissure_couleur" -> moisissure_couleur;
      case "moisissure_noircie" -> moisissure_noircie;
      case "usure" -> usure;
      case "usure_importante" -> usure_importante;
      case "usure_leger" -> usure_leger;
      case "humidite" -> humidite;
      case "humidite_clair" -> humidite_clair;
      case "humidite_intense" -> humidite_intense;
      case "sidewalk", "trottoir" -> sidewalk;
      case "parking" -> parking;
      case "line", "road", "voie_carrossable" -> line;
      case "velux" -> velux;
      case "background" -> background;
      default -> throw new IllegalArgumentException("Unsupported continuation on " + label);
    };
  }
}
