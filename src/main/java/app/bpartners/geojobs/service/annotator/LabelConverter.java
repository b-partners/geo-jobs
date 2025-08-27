package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class LabelConverter implements Function<DetectableType, Label> {

  @Override
  public Label apply(DetectableType detectableType) {
    return new Label()
        .id(randomUUID().toString())
        .color(getColorFromDetectedType(detectableType))
        .name(detectableType.name());
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    return switch (detectableType) {
      case ROAD -> "#de833e";
      case TOITURE_REVETEMENT -> "#DFFF00";
      case PANNEAU_PHOTOVOLTAIQUE -> "#0E4EB3";
      case PISCINE -> "#0DCBD2";
      case PASSAGE_PIETON -> "#F5F586";
      case ARBRE -> "#4BFF33";
      case TROTTOIR -> "#54deb7";
      case LINE -> "#ff3388";
      case ESPACE_VERT -> "#e39724";
      case VOIE_CARROSSABLE -> "TODO";
      case PARKING -> "#8c463e";
      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE -> "#5d8c3e";
      case USURE, USURE_IMPORTANTE, USURE_LEGER -> "#3e718c";
      case FISSURE_CASSURE -> "#733e8c";
      case OBSTACLE -> "#3e8c88";
      case CHEMINEE -> "#a32a55";
      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> "#f2f538";
      case RISQUE_FEU -> "#361c1b";
      case VELUX -> "#c71497";
      case BATI_TUILES -> "#47e66c";
      case BATI_BETON -> "#425c20";
      case BATI_ARDOISE -> "#5299bf";
      case BATI_AUTRES -> "#de6ce0";
      case BATI_ASPHALTE_BITUME -> "#4d4d4d";
      case BATI_BAC_ACIER -> "#708090";
      case BATI_FIBRO_CIMENT -> "#9ca79c";
      case BATI_GRAVIER -> "#b2a89f";
      case BATI_TOLE_ONDULEE -> "#c0c0c0";
      case BATI_ZINC -> "#7f9a9a";
      case TOMBE -> "#6e4b3a";
      case ESPACE_VERT_PARKING -> "#93c47d";
      case BACKGROUND -> null;
    };
  }
}
