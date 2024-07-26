package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import java.util.function.Consumer;

public class CommunityFullDetectionAuthorizer implements Consumer<CreateFullDetection> {
  @Override
  public void accept(CreateFullDetection createFullDetection) {
    var userPrincipal = AuthProvider.getPrincipal();
    if (userPrincipal.isAdmin()) return;

    throw new Error("Not Implemented");
  }
}
