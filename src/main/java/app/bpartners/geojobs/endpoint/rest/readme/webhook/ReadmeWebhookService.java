package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReadmeWebhookService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  public SingleUserInfo retrieveUserInfoByEmail(String email) {
    var user =
        communityAuthorizationRepository
            .findByEmail(email)
            .orElse(CommunityAuthorization.builder().name(email).email(email).build());
    return SingleUserInfo.builder()
        .name(user.getName())
        .email(user.getEmail())
        .apiKey(user.getApiKey())
        .build();
  }
}
