package app.bpartners.geojobs.endpoint.rest.validator;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityFullDetectionAuthorizer;
import app.bpartners.geojobs.model.exception.ApiException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateFullDetectionValidator {
  private CommunityFullDetectionAuthorizer communityFullDetectionAuthorizer;

  public void accept(CreateFullDetection createFullDetection) {
    communityFullDetectionAuthorizer.accept(createFullDetection);
    if (createFullDetection.getEndToEndId() == null) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "You are not allowed to perform the full detection, end to end id was not provided");
    }
  }
}
