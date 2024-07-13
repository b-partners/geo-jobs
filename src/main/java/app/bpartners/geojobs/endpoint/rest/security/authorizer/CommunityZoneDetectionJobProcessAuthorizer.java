package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneDetectionJobProcessAuthorizer
    implements BiConsumer<String, List<DetectableObjectConfiguration>> {
  private final CommunityAuthorizationDetailsRepository cadRepository;
  private final DetectableObjectConfigurationMapper detectableObjectConfigurationMapper;

  @Override
  public void accept(
      String jobId, List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var authorizationDetails = cadRepository.findByApiKey(userPrincipal.getPassword());
    var authorizedDetectableObjectTypes = authorizationDetails.detectableObjectTypes();
    var payloadDetectableTypes =
        detectableObjectConfigurations.stream()
            .map(
                detectableObjectConfiguration ->
                    detectableObjectConfigurationMapper
                        .toDomain(jobId, detectableObjectConfiguration)
                        .getObjectType())
            .toList();
    var notAuthorizedObjectTypes =
        getNotAuthorizedObjectTypes(authorizedDetectableObjectTypes, payloadDetectableTypes);

    if (!notAuthorizedObjectTypes.isEmpty()) {
      throw new ForbiddenException(
          "following objects are not authorized for your community.name = "
              + authorizationDetails.communityName()
              + " : "
              + notAuthorizedObjectTypes);
    }
  }

  private static List<DetectableType> getNotAuthorizedObjectTypes(
      List<DetectableType> authorizedObjectTypes, List<DetectableType> candidateTypes) {
    return candidateTypes.stream().filter(Predicate.not(authorizedObjectTypes::contains)).toList();
  }
}
