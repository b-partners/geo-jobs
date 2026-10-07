package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
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
  private static final DeployedModelCard DAMAGES =
      new DeployedModelCard("damages", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard VEGETATION =
      new DeployedModelCard("vegetation", SEMANTIC_SEGMENTATION, 1, true, 1024, 1024, true);
  private static final DeployedModelCard TOMBS =
      new DeployedModelCard("tombs", OBJECT_DETECTION, 1, false, null, null, null);

  private static final Predicate<DeployedModelCard> ANY = modelCard -> true;

  InferenceModelCardConf conf = mock();
  ModelCardRegistry registry = mock();
  ModelCardResolver subject = new ModelCardResolver(conf, registry);

  @BeforeEach
  void setUp() {
    when(conf.getMappings())
        .thenReturn(
            List.of(
                new ModelCardMapping(HUMIDITE, "damages"),
                new ModelCardMapping(USURE, "damages"),
                new ModelCardMapping(TOMBE, "tombs"),
                new ModelCardMapping(ESPACE_VERT, "vegetation"),
                new ModelCardMapping(ARBRE, "vegetation"),
                new ModelCardMapping(PISCINE, "missing")));
    when(registry.findById("damages")).thenReturn(Optional.of(DAMAGES));
    when(registry.findById("tombs")).thenReturn(Optional.of(TOMBS));
    when(registry.findById("vegetation")).thenReturn(Optional.of(VEGETATION));
    when(registry.findById("missing")).thenReturn(Optional.empty());
  }

  @Test
  void types_sharing_a_model_card_resolve_to_it_once() {
    var resolution = subject.resolve(configs(HUMIDITE, USURE), ANY);

    assertEquals(List.of(DAMAGES), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void resolves_distinct_model_cards_in_request_order() {
    var resolution = subject.resolve(configs(TOMBE, HUMIDITE, USURE), ANY);

    assertEquals(List.of(TOMBS, DAMAGES), resolution.modelCards());
  }

  @Test
  void vegetation_types_resolve_to_their_model_card_without_any_flag() {
    var resolution = subject.resolve(configs(HUMIDITE, ESPACE_VERT, ARBRE), ANY);

    assertEquals(List.of(DAMAGES, VEGETATION), resolution.modelCards());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void reports_unmapped_types() {
    var resolution = subject.resolve(configs(HUMIDITE, DetectableType.CHEMINEE), ANY);

    assertEquals(List.of(DAMAGES), resolution.modelCards());
    assertEquals(Set.of(DetectableType.CHEMINEE), resolution.unmappedTypes());
  }

  @Test
  void incompatible_model_cards_are_reported_as_unsupported_types() {
    var resolution =
        subject.resolve(
            configs(HUMIDITE, USURE, TOMBE),
            modelCard -> !modelCard.modelCardId().equals("damages"));

    assertEquals(List.of(TOMBS), resolution.modelCards());
    assertEquals(Set.of(HUMIDITE, USURE), resolution.unsupportedTypes());
    assertTrue(resolution.unmappedTypes().isEmpty());
  }

  @Test
  void throws_when_mapped_model_card_is_not_deployed() {
    assertThrows(IllegalStateException.class, () -> subject.resolve(configs(PISCINE), ANY));
  }

  private static List<DetectableObjectConfiguration> configs(DetectableType... types) {
    return Arrays.stream(types)
        .<DetectableObjectConfiguration>map(
            type -> DetectableObjectConfiguration.builder().objectType(type).build())
        .toList();
  }
}
