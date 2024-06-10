package app.bpartners.geojobs.endpoint.rest.controller.validator;

import app.bpartners.geojobs.endpoint.rest.model.CreateZoneTilingJob;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CommunityZoneTilingJobProcessValidator
    implements BiConsumer<String, CreateZoneTilingJob> {
  private final CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository;

  @Override
  public void accept(String apiKey, CreateZoneTilingJob createZoneTilingJob) {
    CommunityAuthorizationDetails authenticatedCommunityAuthorizationDetails =
        communityAuthorizationDetailsRepository.findByApiKey(apiKey);
    // TODO: verify it totalAccessibleZone contains thoroughly createZoneTilingJob.getFeatures.
    /*createZoneTilingJob.getFeatures().stream().map(Feature::getGeometry)
    .filter(fg -> authenticatedCommunityAuthorizationDetails.totalAccessibleZone() >=)*/ ;
  }
}
