package app.bpartners.geojobs.model.geometry.route;

import static app.bpartners.geojobs.model.geometry.quadrilateral.model.ContinuationOrientation.lengthOrWidth;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.lumineux;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.obstacle;
import static app.bpartners.geojobs.model.geometry.route.ObjectType.routeTypeFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ObjectTypeTest {

  @Test
  void lumineux_label_resolves_to_a_route_type_like_obstacle() {
    assertEquals(lumineux, routeTypeFrom("lumineux"));
    assertEquals(lumineux, routeTypeFrom("LUMINEUX"));
    assertEquals(obstacle, routeTypeFrom("obstacle"));
    assertEquals(lengthOrWidth, lumineux.continuationOrientation());
  }

  @Test
  void unknown_label_is_still_rejected() {
    assertThrows(IllegalArgumentException.class, () -> routeTypeFrom("unknown_label"));
  }

  @ParameterizedTest
  @CsvSource({
    "road,road",
    "line,road",
    "voie_carrossable,road",
    "pathway,pathway",
    "passage_pieton,pathway",
    "sidewalk,sidewalk",
    "trottoir,sidewalk",
    "tombe,tombe",
    "tomb,tombe",
    "arbre,arbre",
    "tree,arbre",
    "bati_tuiles,bati_tuiles",
    "bati_beton,bati_beton",
    "bati_ardoise,bati_ardoise",
    "bati_autres,bati_autres",
    "toiture_revetement,toiture_revetement",
    "espace_vert,espace_vert",
    "espace_vert_parking,espace_vert_parking",
    "piscine,piscine",
    "pool,piscine",
    "panneau_photovoltaique,panneau_photovoltaique",
    "solar_panel,panneau_photovoltaique",
    "parking,parking",
    "place_standard,place_standard",
    "risque_feu,risque_feu",
    "moisissure,moisissure",
    "moisissure_clair,moisissure_clair",
    "moisissure_couleur,moisissure_couleur",
    "moisissure_noircie,moisissure_noircie",
    "usure,usure",
    "usure_importante,usure_importante",
    "usure_leger,usure_leger",
    "fissure_cassure,fissure_cassure",
    "humidite,humidite",
    "humidite_clair,humidite_clair",
    "humidite_intense,humidite_intense",
    "cheminee,cheminee",
    "cheminée,cheminee",
    "velux,velux",
    "background,background",
    "concession,concession",
    "fleche_directionnelle,fleche_directionnelle",
    "pointilles,pointilles"
  })
  void label_resolves_to_route_type(String label, ObjectType expected) {
    assertEquals(expected, routeTypeFrom(label));
  }

  @Test
  void null_label_resolves_to_null() {
    assertNull(routeTypeFrom((String) null));
  }

  @ParameterizedTest
  @CsvSource({
    "PASSAGE_PIETON,pathway",
    "LINE,road",
    "TROTTOIR,sidewalk",
    "PLACE_STANDARD,place_standard"
  })
  void detectable_object_type_resolves_to_route_type(
      DetectableObjectType detectableObjectType, ObjectType expected) {
    assertEquals(expected, routeTypeFrom(detectableObjectType));
  }

  @ParameterizedTest
  @EnumSource(DetectableObjectType.class)
  void detectable_object_type_without_continuation_is_rejected(
      DetectableObjectType detectableObjectType) {
    var continuable =
        Set.of(
            DetectableObjectType.PASSAGE_PIETON,
            DetectableObjectType.LINE,
            DetectableObjectType.TROTTOIR,
            DetectableObjectType.PLACE_STANDARD);
    org.junit.jupiter.api.Assumptions.assumeFalse(continuable.contains(detectableObjectType));

    assertThrows(IllegalArgumentException.class, () -> routeTypeFrom(detectableObjectType));
  }
}
