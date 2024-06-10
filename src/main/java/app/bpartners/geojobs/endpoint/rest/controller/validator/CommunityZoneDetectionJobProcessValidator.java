package app.bpartners.geojobs.endpoint.rest.controller.validator;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneDetectionJobProcessValidator
    implements BiConsumer<String, List<DetectableObjectConfiguration>> {
  private final CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository;
  private final DetectableObjectTypeMapper objectTypeMapper;

  @Override
  public void accept(
      String apiKey, List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    CommunityAuthorizationDetails authenticatedCommunityAuthorizationDetails =
        communityAuthorizationDetailsRepository.findByApiKey(apiKey);
    HashSet<DetectableObjectType> authorizedDetectableObjectTypes =
        new HashSet<>(authenticatedCommunityAuthorizationDetails.detectableObjects());
    var payloadDetectableTypes =
        detectableObjectConfigurations.stream()
            .map(DetectableObjectConfiguration::getObjectType)
            .map(objectTypeMapper::toRest)
            .toList();
    var notAuthorizedObjectTypes = getNotAuthorizedObjectTypes(authorizedDetectableObjectTypes, payloadDetectableTypes);
    if (!notAuthorizedObjectTypes.isEmpty()) {
      throw new BadRequestException(
          "following objects are not authorized for your community.name = "
              + authenticatedCommunityAuthorizationDetails.communityName()
              + " : "
              + notAuthorizedObjectTypes);
    }
  }

  private static List<DetectableObjectType> getNotAuthorizedObjectTypes(
      HashSet<DetectableObjectType> authorizedObjectTypes, List<DetectableObjectType> candidateTypes) {
    return candidateTypes.stream().filter(Predicate.not(authorizedObjectTypes::contains)).toList();
  }
}
