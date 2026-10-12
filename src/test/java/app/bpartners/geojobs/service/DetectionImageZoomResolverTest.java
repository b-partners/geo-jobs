package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.detection.inference.ModelCardResolver;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionImageZoomResolverTest {
  ModelCardResolver modelCardResolverMock = mock();
  DetectionImageZoomResolver subject = new DetectionImageZoomResolver(modelCardResolverMock);

  @BeforeEach
  void setUp() {
    when(modelCardResolverMock.tilingZoomOf(TOITURE_REVETEMENT)).thenReturn(21);
    when(modelCardResolverMock.tilingZoomOf(USURE)).thenReturn(21);
    when(modelCardResolverMock.tilingZoomOf(ARBRE)).thenReturn(20);
    when(modelCardResolverMock.tilingZoomOf(PISCINE)).thenReturn(22);
  }

  @Test
  void defaults_to_zoom_20_without_any_object_configuration() {
    assertEquals(20, subject.apply(detection(true)));
  }

  @Test
  void uses_the_zoom_of_the_toiture_objects_for_a_toiture_detection() {
    assertEquals(21, subject.apply(detection(true, TOITURE_REVETEMENT, USURE)));
  }

  @Test
  void ignores_the_zoom_of_non_toiture_objects_requested_along_toiture() {
    assertEquals(21, subject.apply(detection(true, TOITURE_REVETEMENT, ARBRE)));
    assertEquals(21, subject.apply(detection(true, USURE, PISCINE)));
  }

  @Test
  void uses_the_highest_zoom_of_the_requested_objects_without_toiture() {
    assertEquals(22, subject.apply(detection(false, ARBRE, PISCINE)));
    assertEquals(20, subject.apply(detection(false, ARBRE)));
  }

  @Test
  void toiture_zoom_is_the_highest_zoom_of_every_toiture_type() {
    assertEquals(21, subject.toitureZoom());
  }

  private Detection detection(boolean toiture, DetectableType... types) {
    var detection = mock(Detection.class);
    when(detection.hasToitureModelName()).thenReturn(toiture);
    when(detection.getDetectableObjectConfigurations())
        .thenReturn(
            Arrays.stream(types)
                .<DetectableObjectConfiguration>map(
                    type -> DetectableObjectConfiguration.builder().objectType(type).build())
                .toList());
    return detection;
  }
}
