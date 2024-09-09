package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FullDetectionAuthorizer implements BiConsumer<CreateFullDetection, Principal> {
  private final CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer;
  private final CommunityAuthorizationRepository caRepository;
  private final CommunityZoneAuthorizer communityZoneAuthorizer;
  private final CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer;

  @Override
  public void accept(CreateFullDetection createFullDetection, Principal principal) {
    var role = principal.getRole();
    switch (role) {
      case ROLE_ADMIN -> {}
      case ROLE_COMMUNITY -> authorizeCommunity(createFullDetection, principal);
      default -> throw new RuntimeException("Unexpected role: " + role);
    }
  }

  private void authorizeCommunity(CreateFullDetection createFullDetection, Principal principal) {
    var features = createFullDetection.getFeatures();
    if (features == null || features.isEmpty()) {
      throw new BadRequestException("You must provide features for your detection");
    }

    var communityAuthorization =
        caRepository.findByApiKey(principal.getPassword()).orElseThrow(ForbiddenException::new);
    communityDetectableObjectTypeAuthorizer.accept(
        communityAuthorization, createFullDetection.getObjectType());
    communityZoneSurfaceAuthorizer.accept(communityAuthorization, features);
    communityZoneAuthorizer.accept(communityAuthorization, features);
  }
}
