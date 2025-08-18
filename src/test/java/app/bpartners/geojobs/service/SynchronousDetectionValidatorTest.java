package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TROTTOIRS;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import org.junit.jupiter.api.Test;

class SynchronousDetectionValidatorTest {

  SynchronousDetectionValidator subject = new SynchronousDetectionValidator();

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
  }
}
