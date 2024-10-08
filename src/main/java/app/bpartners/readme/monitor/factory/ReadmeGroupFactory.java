package app.bpartners.readme.monitor.factory;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;

import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.readme.monitor.model.ReadmeGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadmeGroupFactory {
  private static final String ADMIN_LABEL_NAME = "admin";
  private final CommunityAuthorizationRepository communityAuthRepository;

  public ReadmeGroup createReadmeGroup(Principal principal) {
    if (ROLE_ADMIN.equals(principal.getRole())) {
      return ReadmeGroup.builder().label(ADMIN_LABEL_NAME).apiKey(principal.getPassword()).build();
    }
    var communityAuthorization =
        communityAuthRepository
            .findByApiKey(principal.getPassword())
            .orElseThrow(ForbiddenException::new);

    return ReadmeGroup.builder()
        .apiKey(communityAuthorization.getApiKey())
        .email(communityAuthorization.getEmail())
        .label(communityAuthorization.getName())
        .build();
  }
}
