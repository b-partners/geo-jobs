package app.bpartners.geojobs.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.detection.ModelVersionConf;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateDetectionValidator implements Consumer<CreateDetection> {
  private final ModelVersionConf modelVersionConf;

  @Override
  public void accept(CreateDetection createDetection) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createDetection.getEmailReceiver() == null
        || createDetection.getEmailReceiver().isEmpty()) {
      exceptionMessageBuilder.append("CreateDetection.emailReceiver is mandatory. ");
    }
    if (createDetection.getZoneName() == null || createDetection.getZoneName().isEmpty()) {
      exceptionMessageBuilder.append("CreateDetection.zoneName is mandatory. ");
    }
    if ((createDetection.getDetectableObjectModel() == null
            || createDetection.getDetectableObjectModel().getModelName() == null)
        && (createDetection.getDetectableObjectModelList() == null
            || createDetection.getDetectableObjectModelList().isEmpty())) {
      exceptionMessageBuilder.append(
          "Either CreateDetection.detectableObjectModel or"
              + " CreateDetection.detectableObjectModelList is mandatory.");
    }

    modelsToValidate(createDetection)
        .forEach(model -> appendModelVersionError(model, exceptionMessageBuilder));

    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private List<DetectableObjectModel> modelsToValidate(CreateDetection createDetection) {
    return Stream.concat(
            Stream.ofNullable(createDetection.getDetectableObjectModel()),
            Optional.ofNullable(createDetection.getDetectableObjectModelList()).stream()
                .flatMap(List::stream))
        .toList();
  }

  private void appendModelVersionError(
      DetectableObjectModel model, StringBuilder exceptionMessageBuilder) {
    if (model.getModelName() == null || model.getModelVersion() == null) {
      return;
    }
    if (!modelVersionConf.hasConfiguration(model.getModelName(), model.getModelVersion())) {
      exceptionMessageBuilder.append(
          "Model "
              + model.getModelName().getValue()
              + " has no configuration for version "
              + model.getModelVersion()
              + ". Available versions: "
              + modelVersionConf.getAvailableVersions(model.getModelName())
              + ". ");
    }
  }
}
