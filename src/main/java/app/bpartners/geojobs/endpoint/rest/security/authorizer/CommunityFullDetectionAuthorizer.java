package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import app.bpartners.geojobs.service.FeatureSurfaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class CommunityFullDetectionAuthorizer implements Consumer<CreateFullDetection> {
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final FeatureSurfaceService featureSurfaceService;
  private final CommunityAuthorizationRepository caRepository;
  private final DetectableObjectTypeMapper detectableObjectTypeMapper;

  @Override
  public void accept(CreateFullDetection createFullDetection) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var communityAuthorization = caRepository.findByApiKey(userPrincipal.getPassword()).orElseThrow(ForbiddenException::new);

    verifyObjectTypeToDetect(communityAuthorization, createFullDetection);
    verifyZoneSurfaceToDetect(communityAuthorization, createFullDetection);
    verifyZoneCoordinatesToDetect(communityAuthorization, createFullDetection);
  }

  private void verifyObjectTypeToDetect(CommunityAuthorization communityAuthorization,  CreateFullDetection createFullDetection){
    var candidateObjectType = createFullDetection.getObjectType();
    var authorizedObjectTypes = communityAuthorization
      .getDetectableObjectTypes()
      .stream()
      .map(CommunityDetectableObjectType::getType)
      .map(detectableObjectTypeMapper::toRest)
      .toList();

    if(!authorizedObjectTypes.contains(candidateObjectType)){
      throw new ForbiddenException(
        "following objects are not authorized for your community.name = "
          + communityAuthorization.getName()
          + " : "
          + candidateObjectType);
    }
  }

  private void verifyZoneSurfaceToDetect(CommunityAuthorization communityAuthorization,  CreateFullDetection createFullDetection){
    if(createFullDetection.getFeatures() == null) return;

    var lastUsedSurface = communityUsedSurfaceService.getLastUsedSurfaceByCommunityId(communityAuthorization.getId());
    var newSurfaceValueToDetect = featureSurfaceService.getAreaValue(createFullDetection.getFeatures());
    var candidateSurface = lastUsedSurface.map(communityUsedSurface -> communityUsedSurface.getUsedSurface() + newSurfaceValueToDetect).orElse(newSurfaceValueToDetect);
    var maxAuthorizedSurface = communityAuthorization.getMaxSurface();

    if (maxAuthorizedSurface < candidateSurface) {
      throw new ForbiddenException(
        "Max Surface is exceeded for community.name = "
          + communityAuthorization.getName()
          + " with max allowed surface: "
          + maxAuthorizedSurface);
    }
  }
}
