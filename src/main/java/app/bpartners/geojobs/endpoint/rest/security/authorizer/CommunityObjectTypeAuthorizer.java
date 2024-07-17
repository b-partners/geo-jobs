package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityObjectTypeAuthorizer implements Consumer<List<DetectableType>> {
  private final CommunityAuthorizationRepository caRepository;

  @Override
  public void accept(List<DetectableType> candidateObjectTypes) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var communityAuthorization =
        caRepository.findByApiKey(userPrincipal.getPassword()).orElseThrow(ForbiddenException::new);
    var authorizedObjectTypes =
        communityAuthorization.getDetectableObjectTypes().stream()
            .map(CommunityDetectableObjectType::getType)
            .toList();
    var notAuthorizedObjectTypes =
        getNotAuthorizedObjectTypes(authorizedObjectTypes, candidateObjectTypes);

    if (!notAuthorizedObjectTypes.isEmpty()) {
      throw new ForbiddenException(
          "following objects are not authorized for your community.name = "
              + communityAuthorization.getName()
              + " : "
              + notAuthorizedObjectTypes);
    }
  }

  private static List<DetectableType> getNotAuthorizedObjectTypes(
      List<DetectableType> authorizedTypes, List<DetectableType> candidateTypes) {
    return candidateTypes.stream().filter(Predicate.not(authorizedTypes::contains)).toList();
  }
}
