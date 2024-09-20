package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionAuthorizer implements TriConsumer<String, CreateDetection, Principal> {
  private final CommunityDetectableObjectTypeAuthorizer communityDetectableObjectTypeAuthorizer;
  private final CommunityAuthorizationRepository caRepository;
  private final CommunityZoneAuthorizer communityZoneAuthorizer;
  private final CommunityZoneSurfaceAuthorizer communityZoneSurfaceAuthorizer;
  private final FullDetectionOwnerAuthorizer fullDetectionOwnerAuthorizer;
  private final FullDetectionRepository fullDetectionRepository;

  @Override
  public void accept(String detectionId, CreateDetection createDetection, Principal principal) {
    var role = principal.getRole();
    switch (role) {
      case ROLE_ADMIN -> {}
      case ROLE_COMMUNITY -> authorizeCommunity(detectionId, createDetection, principal);
      default -> throw new RuntimeException("Unexpected role: " + role);
    }
  }

  private void authorizeCommunity(
      String detectionId, CreateDetection createDetection, Principal principal) {
    var features = createDetection.getGeoJsonZone();
    if (features == null || features.isEmpty()) {
      throw new BadRequestException("You must provide features for your detection");
    }
    var communityAuthorization =
        caRepository.findByApiKey(principal.getPassword()).orElseThrow(ForbiddenException::new);
    var fullDetection = fullDetectionRepository.findByEndToEndId(detectionId);
    if (fullDetection.isPresent()) {
      fullDetectionOwnerAuthorizer.accept(communityAuthorization, fullDetection.get());
      return;
    }

    DetectableObjectType candidateObjectType =
        null; // TODO: map objectType from CreateDetection.detectableObjectConfiguration
    communityDetectableObjectTypeAuthorizer.accept(communityAuthorization, candidateObjectType);
    communityZoneSurfaceAuthorizer.accept(communityAuthorization, features);
    communityZoneAuthorizer.accept(communityAuthorization, features);
  }

  private List<DetectableObjectType> mapFromModel(Object o) {
    var objectTypes = new ArrayList<DetectableObjectType>();
    if (o instanceof BPToitureModel) {
      BPToitureModel model = (BPToitureModel) o;
      if (model.getArbre().booleanValue()) {
        // objectTypes.add(); TODO add ARBRE
      }
    }
    throw new ApiException(SERVER_EXCEPTION, "Unknown instance of object " + o.getClass());
  }
}
