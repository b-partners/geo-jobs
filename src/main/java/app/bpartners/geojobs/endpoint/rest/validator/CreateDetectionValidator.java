package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CreateDetectionValidator implements Consumer<CreateDetection> {
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
    if (createDetection.getDetectableObjectModel() == null
        || createDetection.getDetectableObjectModel().getModelName() == null) {
      exceptionMessageBuilder.append("CreateDetection.detectableObjectModel is mandatory.");
    }
    if (createDetection.getNeedsImageOutput() != null
        && createDetection.getNeedsImageOutput()
        && createDetection.getGeoJsonZone() != null) {
      var providedPoints =
          createDetection.getGeoJsonZone().stream()
              .filter(
                  feature ->
                      feature.getGeometry() != null
                          && feature.getGeometry().getActualInstance() instanceof Point)
              .toList();
      if (providedPoints.size() > 1) {
        throw new NotImplementedException(
            "Only one point is supported to generate image, otherwise actual provided geojson are "
                + providedPoints.size()
                + " points : "
                + providedPoints.stream()
                    .map(
                        feature ->
                            "("
                                + feature.getGeometry().getPoint().getCoordinates().getFirst()
                                + ", "
                                + feature.getGeometry().getPoint().getCoordinates().getLast()
                                + ") ")
                    .collect(Collectors.toSet()));
      }
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
