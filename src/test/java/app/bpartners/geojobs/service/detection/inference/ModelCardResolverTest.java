package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.CHEMINEE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ESPACE_VERT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.OBJECT_DETECTION;
import static app.bpartners.geojobs.service.detection.inference.DeployedModelCard.ProblemType.SEMANTIC_SEGMENTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModelCardResolverTest {
  private static final Predicate<DeployedModelCard> ANY = modelCard -> true;
  private static final int DEFAULT_ZOOM = 20;
  private static final int DAMAGES_ZOOM = 21;
  private static final DeployedModelCard DAMAGES =
      new DeployedModelCard("damages", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard VEGETATION =
      new DeployedModelCard("vegetation", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard TOMBS =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, null);
  private static final ResolvedModelCard RESOLVED_DAMAGES =
      new ResolvedModelCard(DAMAGES, DAMAGES_ZOOM, 5.0);
  private static final ResolvedModelCard RESOLVED_TOMBS =
      new ResolvedModelCard(TOMBS, DEFAULT_ZOOM, 10.0);
  private static final ResolvedModelCard RESOLVED_VEGETATION =
      new ResolvedModelCard(VEGETATION, DEFAULT_ZOOM, 10.0);

  InferenceModelCardConf conf = mock();
  ModelCardRegistry registry = mock();
  ModelCardResolver subject = new ModelCardResolver(conf, registry);

  @BeforeEach
  void setUp() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(HUMIDITE, "damages", DAMAGES_ZOOM, 5.0),
                new ModelCardMapping(USURE, "damages", DAMAGES_ZOOM, 5.0),
                new ModelCardMapping(TOMBE, "tombs", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(ESPACE_VERT, "vegetation", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(ARBRE, "vegetation", DEFAULT_ZOOM, 10.0),
                new ModelCardMapping(PISCINE, "missing", DAMAGES_ZOOM, 5.0)));
    when(registry.findById("damages")).thenReturn(Optional.of(DAMAGES));
    when(registry.findById("tombs")).thenReturn(Optional.of(TOMBS));
    when(registry.findById("vegetation")).thenReturn(Optional.of(VEGETATION));
    when(registry.findById("missing")).thenReturn(Optional.empty());
  }

  @Test
  void types_sharing_a_model_card_resolve_to_it_once_with_its_zoom_and_resolution() {
    var resolution = subject.resolve(configs(HUMIDITE, USURE), DAMAGES_ZOOM, ANY);

    assertEquals(List.of(RESOLVED_DAMAGES), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void resolves_only_the_model_cards_of_the_tile_zoom() {
    var configs = configs(TOMBE, HUMIDITE, USURE);

    assertEquals(
        List.of(RESOLVED_TOMBS), subject.resolve(configs, DEFAULT_ZOOM, ANY).modelCards());
    assertEquals(
        List.of(RESOLVED_DAMAGES), subject.resolve(configs, DAMAGES_ZOOM, ANY).modelCards());
  }

  @Test
  void vegetation_types_resolve_to_their_model_card_without_any_flag() {
    var resolution = subject.resolve(configs(HUMIDITE, ESPACE_VERT, ARBRE), DEFAULT_ZOOM, ANY);

    assertEquals(List.of(RESOLVED_VEGETATION), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void reports_unmapped_types_only_on_the_default_zoom() {
    var configs = configs(HUMIDITE, CHEMINEE);

    assertEquals(
        Set.of(CHEMINEE), subject.resolve(configs, DEFAULT_ZOOM, ANY).unmappedTypes());
    assertTrue(subject.resolve(configs, DAMAGES_ZOOM, ANY).unmappedTypes().isEmpty());
  }

  @Test
  void incompatible_model_cards_are_reported_as_unsupported_types() {
    var resolution =
        subject.resolve(
            configs(HUMIDITE, USURE, TOMBE),
            DAMAGES_ZOOM,
            modelCard -> !modelCard.modelCardId().equals("damages"));

    assertTrue(resolution.modelCards().isEmpty());
    assertEquals(Set.of(HUMIDITE, USURE), resolution.unsupportedTypes());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void throws_when_mapped_model_card_is_not_deployed() {
    assertThrows(
        IllegalStateException.class,
        () -> subject.resolve(configs(PISCINE), DAMAGES_ZOOM, ANY));
  }

  @Test
  void tiling_zoom_levels_are_distinct_sorted_and_default_for_unmapped_types() {
    assertEquals(List.of(20, 21), subject.tilingZoomLevels(configs(HUMIDITE, TOMBE, CHEMINEE)));
    assertEquals(List.of(21), subject.tilingZoomLevels(configs(HUMIDITE, USURE)));
    assertEquals(List.of(DEFAULT_ZOOM), subject.tilingZoomLevels(configs(CHEMINEE)));
    assertEquals(List.of(DEFAULT_ZOOM), subject.tilingZoomLevels(List.of()));
  }

  @Test
  void tiling_zoom_of_a_type() {
    assertEquals(DAMAGES_ZOOM, subject.tilingZoomOf(HUMIDITE));
    assertEquals(DEFAULT_ZOOM, subject.tilingZoomOf(CHEMINEE));
  }

  private static List<DetectableObjectConfiguration> configs(DetectableType... types) {
    return Arrays.stream(types)
        .<DetectableObjectConfiguration>map(
            type -> DetectableObjectConfiguration.builder().objectType(type).build())
        .toList();
  }
}
