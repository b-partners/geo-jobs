package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.ARBRE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.FeatureRoofResultPropertiesComputer;
import app.bpartners.geojobs.service.area.mutation.MutationContextFactory;
import app.bpartners.geojobs.service.area.mutation.model.AreaPictureHistoryResponse;
import app.bpartners.geojobs.service.area.mutation.model.MutationContext;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.geojson.GeometryCorrector;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

class DetectionPropertiesServiceTest {
  private final MutationContextFactory mutationContextFactoryMock = mock();

  @Test
  void vegetation_is_requested_when_a_vegetation_type_is_among_the_job_configurations() {
    var detection =
        Detection.builder()
            .detectableObjectConfigurations(
                List.of(configurationOf(TOITURE_REVETEMENT), configurationOf(ARBRE)))
            .build();

    assertTrue(DetectionPropertiesService.isVegetationRequested(detection));
  }

  @Test
  void vegetation_is_not_requested_for_a_roof_only_job() {
    var roofOnly =
        Detection.builder()
            .detectableObjectConfigurations(List.of(configurationOf(TOITURE_REVETEMENT)))
            .build();
    var noConfiguration = Detection.builder().build();

    assertFalse(DetectionPropertiesService.isVegetationRequested(roofOnly));
    assertFalse(DetectionPropertiesService.isVegetationRequested(noConfiguration));
  }

  private static DetectableObjectConfiguration configurationOf(DetectableType objectType) {
    return DetectableObjectConfiguration.builder().objectType(objectType).build();
  }

  private DetectionPropertiesService subjectWithMutation(boolean mutationEnabled) {
    return new DetectionPropertiesService(
        mock(DetectionRepository.class),
        mock(FeatureRoofResultPropertiesComputer.class),
        mock(GeometryConverter.class),
        mock(MachineDetectedTileRepository.class),
        mock(GeometryCorrector.class),
        mutationContextFactoryMock,
        mutationEnabled);
  }

  @Test
  void tryCreateMutationContext_returns_the_built_context_on_success_when_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    var expected =
        new MutationContext(
            new AreaPictureHistoryResponse.DatedImage(
                2022, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/old.jpg")),
            new AreaPictureHistoryResponse.DatedImage(
                2024, new AreaPictureHistoryResponse.PresignedUrl("https://geodata.test/new.jpg")),
            new File("mask.png"));
    when(mutationContextFactoryMock.create(detection, roofGeometry)).thenReturn(expected);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertEquals(expected, actual);
  }

  @Test
  void
      tryCreateMutationContext_returns_null_instead_of_propagating_when_factory_throws_and_mutation_is_enabled() {
    var subject = subjectWithMutation(true);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);
    when(mutationContextFactoryMock.create(detection, roofGeometry))
        .thenThrow(new IllegalStateException("geodata API unreachable"));

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
  }

  @Test
  void tryCreateMutationContext_does_not_call_the_factory_when_mutation_is_disabled() {
    var subject = subjectWithMutation(false);
    var detection = Detection.builder().id("detection-1").build();
    var roofGeometry = mock(Geometry.class);

    var actual = invokeTryCreateMutationContext(subject, detection, roofGeometry);

    assertNull(actual);
    verifyNoInteractions(mutationContextFactoryMock);
  }

  @SneakyThrows
  private Object invokeTryCreateMutationContext(
      DetectionPropertiesService subject, Detection detection, Geometry roofGeometry) {
    Method method =
        DetectionPropertiesService.class.getDeclaredMethod(
            "tryCreateMutationContext", Detection.class, Geometry.class);
    method.setAccessible(true);
    return method.invoke(subject, detection, roofGeometry);
  }
}
