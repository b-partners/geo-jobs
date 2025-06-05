package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TROTTOIRS;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SynchronousDetectionValidatorTest {
  GeometryTiledValidator geometryTiledValidator = mock();

  SynchronousDetectionValidator subject = new SynchronousDetectionValidator(geometryTiledValidator);

  @Test
  void accept_throwsNotImplementedException_whenModelNameIsNotToiture() {
    var createDetectionMock = mock(CreateDetection.class);
    var detectableObjectModelMock = mock(DetectableObjectModel.class);
    when(createDetectionMock.getDetectableObjectModel()).thenReturn(detectableObjectModelMock);
    when(detectableObjectModelMock.getModelName()).thenReturn(TROTTOIRS);
    NotImplementedException thrown =
        assertThrows(NotImplementedException.class, () -> subject.accept(createDetectionMock));

    assertTrue(
        thrown.getMessage().contains("Only BP_TOITURE detection model is supported for now"));
    assertTrue(thrown.getMessage().contains("otherwise, model provided is " + TROTTOIRS));

    verify(createDetectionMock, times(1)).getDetectableObjectModel();
    verify(detectableObjectModelMock, times(1)).getModelName();
    verifyNoMoreInteractions(detectableObjectModelMock, geometryTiledValidator);
  }

  @Test
  void accept_throwsNotImplementedException_whenGeoJsonZoneIsEmpty() {
    var createDetectionMock = mock(CreateDetection.class);
    var detectableObjectModelMock = mock(DetectableObjectModel.class);

    when(createDetectionMock.getDetectableObjectModel()).thenReturn(detectableObjectModelMock);
    when(detectableObjectModelMock.getModelName()).thenReturn(TOITURE);
    when(createDetectionMock.getGeoJsonZone()).thenReturn(Collections.emptyList());

    NotImplementedException thrown =
        assertThrows(NotImplementedException.class, () -> subject.accept(createDetectionMock));

    assert (thrown.getMessage().contains("Only one feature supported"));
    assert (thrown.getMessage().contains("provided geoJson features.size = 0"));

    verify(createDetectionMock, times(1)).getDetectableObjectModel();
    verify(detectableObjectModelMock, times(1)).getModelName();
    verify(createDetectionMock, times(1)).getGeoJsonZone();
    verifyNoMoreInteractions(
        createDetectionMock, detectableObjectModelMock, geometryTiledValidator);
  }

  @Test
  void accept_throwsNotImplementedException_whenGeoJsonIsNotContainedInFrame() {
    var createDetectionMock = mock(CreateDetection.class);
    var detectableObjectModelMock = mock(DetectableObjectModel.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var geometryMock = mock(Geometry.class);

    when(createDetectionMock.getDetectableObjectModel()).thenReturn(detectableObjectModelMock);
    when(detectableObjectModelMock.getModelName()).thenReturn(TOITURE);
    when(createDetectionMock.getGeoJsonZone()).thenReturn(List.of(featureMock));
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(geometryMock);
    when(geometryTiledValidator.apply(geometryMock)).thenReturn(false);

    NotImplementedException thrown =
        assertThrows(NotImplementedException.class, () -> subject.accept(createDetectionMock));

    assertTrue(
        thrown
            .getMessage()
            .contains("Provided geojson polygon is too large to be processed synchronously"));

    verify(createDetectionMock, times(1)).getDetectableObjectModel();
    verify(detectableObjectModelMock, times(1)).getModelName();
    verify(createDetectionMock, times(1)).getGeoJsonZone();
    verify(featureMock, times(1)).getGeometry();
    verify(featureGeometryMock, times(1)).getActualInstance();
    verify(geometryTiledValidator, times(1)).apply(geometryMock);
    verifyNoMoreInteractions(
        createDetectionMock,
        detectableObjectModelMock,
        featureMock,
        featureGeometryMock,
        geometryMock,
        geometryTiledValidator);
  }
}
