package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import app.bpartners.geojobs.service.FeatureSurfaceService;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityZoneSurfaceAuthorizer
    implements BiConsumer<CommunityAuthorization, List<Feature>> {
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final FeatureSurfaceService featureSurfaceService;

  @Override
  public void accept(
      CommunityAuthorization communityAuthorization, List<Feature> candidateFeatures) {
    var totalUsedSurface =
        communityUsedSurfaceService.getTotalUsedSurfaceByCommunityId(
            communityAuthorization.getId(), SQUARE_DEGREE);
    var newSurfaceValueToDetect = featureSurfaceService.getAreaValue(candidateFeatures);
    var candidateSurface =
        totalUsedSurface
            .map(
                communityUsedSurface ->
                    communityUsedSurface.getUsedSurface() + newSurfaceValueToDetect)
            .orElse(newSurfaceValueToDetect);
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
