package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.model.parcelization.area.AreaComputer;
import app.bpartners.geojobs.model.parcelization.area.SquareDegree;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import jakarta.ws.rs.NotSupportedException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneTilingJobProcessAuthorizer implements Consumer<CreateZoneTilingJob> {
  private final CommunityAuthorizationDetailsRepository cadRepository;
  private static final AreaComputer areaComputer = new AreaComputer();
  private final FeatureMapper featureMapper;

  @Override
  public void accept(CreateZoneTilingJob createZoneTilingJob) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    var authorizationDetails = cadRepository.findByApiKey(userPrincipal.getPassword());
    var authorizedZoneNames = authorizationDetails.authorizedZoneNames();
    var maxAuthorizedSurface = authorizationDetails.maxSurface();

    var payloadZoneName = createZoneTilingJob.getZoneName();
    var payloadSurface = getSurfaceFromCreateZoneTilingJob(createZoneTilingJob);

    if (payloadZoneName == null || !authorizedZoneNames.contains(payloadZoneName)) {
      throw new ForbiddenException(
          "following zoneName is not authorized for your community.name = "
              + authorizationDetails.communityName()
              + " : "
              + payloadZoneName);
    }

    if (maxAuthorizedSurface < payloadSurface) {
      throw new ForbiddenException(
          "Max Surface is exceeded for community.name = "
              + authorizationDetails.communityName()
              + " with max allowed surface: "
              + maxAuthorizedSurface);
    }
  }

  private double getSurfaceFromCreateZoneTilingJob(CreateZoneTilingJob createZoneTilingJob) {
    if (createZoneTilingJob.getFeatures() == null) return 0;

    return createZoneTilingJob.getFeatures().stream()
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
  }
}
