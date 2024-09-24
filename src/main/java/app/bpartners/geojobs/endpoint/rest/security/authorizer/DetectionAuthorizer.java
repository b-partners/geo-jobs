package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateDetection;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import java.util.Objects;
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
  private final DetectionOwnerAuthorizer detectionOwnerAuthorizer;
  private final DetectionRepository detectionRepository;
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;

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
    var communityAuthorization =
        caRepository.findByApiKey(principal.getPassword()).orElseThrow(ForbiddenException::new);
    var optionalDetection = detectionRepository.findByEndToEndId(detectionId);
    if (optionalDetection.isPresent()) {
      detectionOwnerAuthorizer.accept(communityAuthorization, optionalDetection.get());
      return;
    }

    var features = createDetection.getGeoJsonZone();
    if (features != null && !features.isEmpty()) {
      communityZoneSurfaceAuthorizer.accept(communityAuthorization, features);
      communityZoneAuthorizer.accept(communityAuthorization, features);
    }
    var detectableObjects =
        detectableObjectTypeMapper.mapFromModel(
            Objects.requireNonNull(createDetection.getDetectableObjectModel()).getActualInstance());
    detectableObjects.forEach(
        candidateObjectType ->
            communityDetectableObjectTypeAuthorizer.accept(
                communityAuthorization, candidateObjectType));
  }
}
