package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.model.security.ApiKey;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final UserAccountsApi userAccountsApi;
  private final String adminApiKey;

  public ApiKeyService(
      CommunityAuthorizationRepository communityAuthorizationRepository,
      UserAccountsApi userAccountsApi,
      @Value("${admin.api.key}") String adminApiKey) {
    this.communityAuthorizationRepository = communityAuthorizationRepository;
    this.userAccountsApi = userAccountsApi;
    this.adminApiKey = adminApiKey;
  }

  public List<ApiKey> generateApiKeys(List<CommunityAuthorization> authorizations) {
    var communityAuthorizations = handleExistingCommunities(authorizations);

    communityAuthorizations.forEach(
        authorization -> {
          var dashboardUserApiKeyList =
              userAccountsApi.getOrGenerateApiKey(
                  authorization.getEmail(), authorization.getApiKey(), adminApiKey);
          authorization.setDashboardApiKey(dashboardUserApiKeyList.key());
          generateApiKey(authorization);
        });

    return communityAuthorizationRepository.saveAll(communityAuthorizations).stream()
        .map(
            communityAuthorization ->
                new ApiKey(
                    communityAuthorization.getMostRecentApiKey().getKeyValue(),
                    communityAuthorization.getMostRecentApiKey().getCreationDatetime()))
        .toList();
  }

  private void generateApiKey(CommunityAuthorization authorization) {
    CommunityAuthorizationApiKey newApiKey =
        CommunityAuthorizationApiKey.builder()
            .id(randomUUID().toString())
            .keyValue(randomUUID().toString())
            .creationDatetime(now())
            .communityOwnerId(authorization.getId())
            .build();

    List<CommunityAuthorizationApiKey> newApiKeys = new ArrayList<>(authorization.getApiKeys());
    newApiKeys.add(newApiKey);

    authorization.setApiKeys(newApiKeys);
  }

  private List<CommunityAuthorization> handleExistingCommunities(
      List<CommunityAuthorization> authorizations) {
    return authorizations.stream()
        .map(
            newCommunity -> {
              var optionalCommunityAuthorization =
                  communityAuthorizationRepository.findByEmail(newCommunity.getEmail());
              if (optionalCommunityAuthorization.isPresent()) {
                var existingCommunity = optionalCommunityAuthorization.get();
                existingCommunity
                    .getApiKeys()
                    .add(
                        CommunityAuthorizationApiKey.builder()
                            .id(randomUUID().toString())
                            .keyValue(newCommunity.getApiKey())
                            .communityOwnerId(existingCommunity.getId())
                            .creationDatetime(now())
                            .build());
                return existingCommunity;
              }
              return newCommunity;
            })
        .toList();
  }
}
