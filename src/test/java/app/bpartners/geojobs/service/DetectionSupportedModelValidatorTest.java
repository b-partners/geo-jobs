package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionSupportedModelValidatorTest {

  DetectionSupportedModelValidator subject = new DetectionSupportedModelValidator();

  @Test
  void accept_ok() {
    Detection detection = mock();
    DetectableObjectModel detectableObjectModel = mock();

    when(detectableObjectModel.getModelName()).thenReturn(ModelName.TOITURE);
    when(detection.getDetectableObjectModel()).thenReturn(detectableObjectModel);
    when(detection.getDetectableObjectModelList()).thenReturn(validDetectableObjectModelList());

    subject.accept(detection);

    verify(detection, times(3)).getDetectableObjectModel();
    verify(detection, times(1)).getDetectableObjectModelList();
    verify(detection, times(1)).getDetectableObjectModelList();
    verify(detectableObjectModel, times(2)).getModelName();
  }

  @Test
  void accept_ko() {
    Detection detection = mock();

    when(detection.getDetectableObjectModel())
        .thenReturn(notValidDetectableObjectModelList().getFirst());
    when(detection.getDetectableObjectModelList()).thenReturn(notValidDetectableObjectModelList());

    assertThrows(UnsupportedOperationException.class, () -> subject.accept(detection));
  }

  private List<DetectableObjectModel> validDetectableObjectModelList() {
    return List.of(
        new DetectableObjectModel().modelName(ModelName.TOITURE),
        new DetectableObjectModel().modelName(ModelName.VEGETATION));
  }

  private List<DetectableObjectModel> notValidDetectableObjectModelList() {
    return DetectionSupportedModelValidator.UNSUPPORTED_MODELS.stream()
        .map(modelName -> new DetectableObjectModel().modelName(modelName))
        .toList();
  }
}
