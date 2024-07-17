package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityZone;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityZoneAuthorizer implements Consumer<String> {
  private final CommunityAuthorizationRepository caRepository;

  @Override
  public void accept(String candidateZoneName) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var communityAuthorization =
        caRepository.findByApiKey(userPrincipal.getPassword()).orElseThrow(ForbiddenException::new);
    var authorizedZoneNames =
        communityAuthorization.getAuthorizedZones().stream().map(CommunityZone::getName).toList();

    if (candidateZoneName == null || !authorizedZoneNames.contains(candidateZoneName)) {
      throw new ForbiddenException(
          "following zoneName is not authorized for your community.name = "
              + communityAuthorization.getName()
              + " : "
              + candidateZoneName);
    }
  }
}
