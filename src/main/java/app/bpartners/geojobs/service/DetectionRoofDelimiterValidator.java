package app.bpartners.geojobs.service;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class DetectionRoofDelimiterValidator implements Consumer<Detection> {

  @Override
  public void accept(Detection detection) {
    var detectionId = detection.getId();
    var exceptionMessageBuilder = new StringBuilder();
    if (detection.getImageFileKey() == null) {
      exceptionMessageBuilder
          .append(
              "Detection.image is mandatory before configuring root delimiter otherwise actual"
                  + " detection.id=")
          .append(detectionId)
          .append(" does not have image. ");
    }
    if (detection.getPolygonRoofDelimitation() != null
        && !detection.getPolygonRoofDelimitation().isEmpty()) {
      exceptionMessageBuilder
          .append("Detection.id=")
          .append(detectionId)
          .append(" roofDelimiter.polygon already set to ")
          .append(detection.getPolygonRoofDelimitation())
          .append(". ");
    }
    if (detection.isSucceeded()) {
      exceptionMessageBuilder
          .append("Detection.id=")
          .append(detectionId)
          .append(" is already succeeded and can't be launched anymore.");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
