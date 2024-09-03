package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
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
    if (principal.isAdmin()) return;

    var communityAuthorization =
        caRepository.findByApiKey(principal.getPassword()).orElseThrow(ForbiddenException::new);

    communityDetectableObjectTypeAuthorizer.accept(
        communityAuthorization, createFullDetection.getObjectType());
    if (createFullDetection.getFeatures() != null) {
      communityZoneSurfaceAuthorizer.accept(
          communityAuthorization, createFullDetection.getFeatures());
      communityZoneAuthorizer.accept(communityAuthorization, createFullDetection.getFeatures());
    }
  }
}
