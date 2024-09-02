package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityFullDetectionAuthorizer;
import app.bpartners.geojobs.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateFullDetectionValidator {
  private CommunityFullDetectionAuthorizer communityFullDetectionAuthorizer;

  public void accept(CreateFullDetection createFullDetection) {
    communityFullDetectionAuthorizer.accept(createFullDetection);
    if (createFullDetection == null) {
      throw new BadRequestException("CreateFullDetection must not be null");
    }
    StringBuilder sb = new StringBuilder();
    if (createFullDetection.getEndToEndId() == null) {
      sb.append(
          "You are not allowed to perform the full detection, end to end id was not provided. ");
    }
    var detectableObjectConfigurations = createFullDetection.getDetectableObjectConfigurations();
    if (detectableObjectConfigurations == null) {
      sb.append("DetectableObjectConfigurations is mandatory. ");
    } else {
      if (detectableObjectConfigurations.size() != 1) {
        sb.append("Only unique detectableObjectConfigurations is handle for now. ");
      }
    }
    var exceptionMsg = sb.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
