package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthentication;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneDetectionJobProcessAuthorizer
    implements Consumer<List<DetectableObjectConfiguration>> {
  private final CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository;
  private final DetectableObjectTypeMapper objectTypeMapper;

  @Override
  public void accept(List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    ApiKeyAuthentication apiKeyAuthentication =
        ApiKeyAuthenticationFilter.getApiKeyAuthentication();
    if (apiKeyAuthentication.isAdmin()) {
      return;
    }

    CommunityAuthorizationDetails authenticatedCommunityAuthorizationDetails =
        communityAuthorizationDetailsRepository.findByApiKey(apiKeyAuthentication.getApiKey());
    List<DetectableType> authorizedDetectableObjectTypes =
        authenticatedCommunityAuthorizationDetails.detectableObjectTypes();
    List<DetectableType> payloadDetectableTypes =
        detectableObjectConfigurations.stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .toList();
    var notAuthorizedObjectTypes =
        getNotAuthorizedObjectTypes(authorizedDetectableObjectTypes, payloadDetectableTypes);
    if (!notAuthorizedObjectTypes.isEmpty()) {
      throw new ForbiddenException(
          "following objects are not authorized for your community.name = "
              + authenticatedCommunityAuthorizationDetails.communityName()
              + " : "
              + notAuthorizedObjectTypes);
    }
  }

  private static List<DetectableType> getNotAuthorizedObjectTypes(
      List<DetectableType> authorizedObjectTypes, List<DetectableType> candidateTypes) {
    return candidateTypes.stream().filter(Predicate.not(authorizedObjectTypes::contains)).toList();
  }
}
