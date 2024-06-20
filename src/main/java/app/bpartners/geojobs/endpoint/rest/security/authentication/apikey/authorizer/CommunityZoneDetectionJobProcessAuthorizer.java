package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthentication;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
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
    List<DetectableObjectType> authorizedDetectableObjectTypes =
        authenticatedCommunityAuthorizationDetails.detectableObjectTypes();
    List<DetectableObjectType> payloadDetectableTypes =
        detectableObjectConfigurations.stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .map(objectTypeMapper::toRest)
            .toList();
    var notAuthorizedObjectTypes =
        getNotAuthorizedObjectTypes(authorizedDetectableObjectTypes, payloadDetectableTypes);
    if (!notAuthorizedObjectTypes.isEmpty()) {
      throw new BadRequestException(
          "following objects are not authorized for your community.name = "
              + authenticatedCommunityAuthorizationDetails.communityName()
              + " : "
              + notAuthorizedObjectTypes);
    }
  }

  private static List<DetectableObjectType> getNotAuthorizedObjectTypes(
      List<DetectableObjectType> authorizedObjectTypes, List<DetectableObjectType> candidateTypes) {
    return candidateTypes.stream().filter(Predicate.not(authorizedObjectTypes::contains)).toList();
  }
}
