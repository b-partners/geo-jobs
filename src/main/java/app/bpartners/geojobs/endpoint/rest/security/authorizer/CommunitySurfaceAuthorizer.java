package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.model.parcelization.area.AreaComputer;
import app.bpartners.geojobs.model.parcelization.area.SquareDegree;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.CommunityUsedSurface;
import app.bpartners.geojobs.service.CommunityUsedSurfaceService;
import jakarta.ws.rs.NotSupportedException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunitySurfaceAuthorizer implements Consumer<List<Feature>> {
  private final CommunityAuthorizationRepository caRepository;
  private final CommunityUsedSurfaceService communityUsedSurfaceService;
  private final FeatureMapper featureMapper;
  private static final AreaComputer areaComputer = new AreaComputer();

  @Override
  public void accept(List<Feature> features) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var communityAuthorization =
        caRepository.findByApiKey(userPrincipal.getPassword()).orElseThrow(ForbiddenException::new);
    var maxAuthorizedSurface = communityAuthorization.getMaxSurface();
    var lastUsedSurface =
        communityUsedSurfaceService.getLastUsedSurfaceByApiKey(userPrincipal.getPassword());
    var totalUsedSurface = calcTotalUsedSurface(lastUsedSurface, features);

    if (maxAuthorizedSurface < totalUsedSurface) {
      throw new ForbiddenException(
          "Max Surface is exceeded for community.name = "
              + communityAuthorization.getName()
              + " with max allowed surface: "
              + maxAuthorizedSurface);
    }
  }

  private double calcTotalUsedSurface(
      Optional<CommunityUsedSurface> lastUsedSurface, List<Feature> features) {
    var featureSurface =
        features.stream()
            .map(polygon -> areaComputer.apply(featureMapper.toDomain(polygon)))
            .mapToDouble(
                area -> {
                  if (!(area instanceof SquareDegree)) {
                    throw new NotSupportedException();
                  }
                  return ((SquareDegree) area).getValue();
                })
            .reduce(Double::sum)
            .orElse(0);
    return featureSurface + lastUsedSurface.map(CommunityUsedSurface::getUsedSurface).orElse(0.0);
  }
}
