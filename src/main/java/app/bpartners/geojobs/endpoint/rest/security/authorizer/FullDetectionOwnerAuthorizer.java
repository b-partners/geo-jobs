package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FullDetectionOwnerAuthorizer
    implements BiConsumer<CommunityAuthorization, FullDetection> {
  @Override
  public void accept(CommunityAuthorization communityAuthorization, FullDetection fullDetection) {
    if (!communityAuthorization.getId().equals(fullDetection.getCommunityOwnerId())) {
      throw new ForbiddenException(
          "Given endToEndId is not authorized for your community.name = "
              + fullDetection.getCommunityOwnerId()
              + ", endToEndId = "
              + fullDetection.getEndToEndId());
    }
  }
}
