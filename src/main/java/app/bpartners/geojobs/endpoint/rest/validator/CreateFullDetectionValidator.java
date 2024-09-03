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
    if (createFullDetection.getEndToEndId() == null) {
      throw new BadRequestException("You must provide an end-to-end id for your detection");
    }

    communityFullDetectionAuthorizer.accept(createFullDetection);
  }
}
