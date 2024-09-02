package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityFullDetectionAuthorizer implements Consumer<CreateFullDetection> {
  private final CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer;
  private final CommunityAuthorizationRepository caRepository;
  private final CommunityZoneAuthorizer communityZoneAuthorizer;
  private final CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer;

  @Override
  public void accept(CreateFullDetection createFullDetection) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var communityAuthorization =
        caRepository.findByApiKey(userPrincipal.getPassword()).orElseThrow(ForbiddenException::new);

    var detectableObjectConfiguration =
        createFullDetection.getDetectableObjectConfigurations().getFirst();
    communityDetectableObjectTypeAuthorizer.accept(
        communityAuthorization, detectableObjectConfiguration.getType());
    if (createFullDetection.getFeatures() != null) {
      communityZoneSurfaceAuthorizer.accept(
          communityAuthorization, createFullDetection.getFeatures());
      communityZoneAuthorizer.accept(communityAuthorization, createFullDetection.getFeatures());
    }
  }
}
