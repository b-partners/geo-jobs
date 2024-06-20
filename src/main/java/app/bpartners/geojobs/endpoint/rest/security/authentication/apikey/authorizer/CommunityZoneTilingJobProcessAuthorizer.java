package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.authorizer;

import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthentication;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneTilingJobProcessAuthorizer implements Consumer<ZoneTilingJob> {
  private final CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository;

  @Override
  public void accept(ZoneTilingJob createZoneTilingJob) {
    ApiKeyAuthentication apiKeyAuthentication =
        ApiKeyAuthenticationFilter.getApiKeyAuthentication();
    if (apiKeyAuthentication.isAdmin()) {
      return;
    }

    CommunityAuthorizationDetails authenticatedCommunityAuthorizationDetails =
        communityAuthorizationDetailsRepository.findByApiKey(apiKeyAuthentication.getApiKey());
    List<String> authorizedZoneNames =
        authenticatedCommunityAuthorizationDetails.authorizedZoneNames();
    String payloadZoneName = createZoneTilingJob.getZoneName();

    if (payloadZoneName == null || !isAuthorizedZoneName(authorizedZoneNames, payloadZoneName)) {
      throw new ForbiddenException(
          "following zoneName is not authorized for your community.name = "
              + authenticatedCommunityAuthorizationDetails.communityName()
              + " : "
              + payloadZoneName);
    }
  }

  private boolean isAuthorizedZoneName(List<String> authorizedZoneNames, String candidateZoneName) {
    return authorizedZoneNames.stream().anyMatch(candidateZoneName::equals);
  }
}
