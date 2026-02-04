package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.SurfaceUnit.SQUARE_DEGREE;
import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorizationApiKey;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ApiKeyServiceIT extends FacadeIT {
  static String ADMIN_API_KEY = randomUUID().toString();

  @MockBean UserAccountsApi userAccountsApiMock;
  @Autowired CommunityAuthorizationRepository communityAuthorizationRepository;
  ApiKeyService subject;

  @Test
  void new_api_key_created_when_new_community_used_on_generate_keys() {
    when(userAccountsApiMock.getOrGenerateApiKey(any(), any(), any()))
        .thenReturn(new UserApiKey(randomUUID().toString(), DASHBOARD));

    subject =
        new ApiKeyService(communityAuthorizationRepository, userAccountsApiMock, ADMIN_API_KEY);

    CommunityAuthorization newCommunityAuthorization =
        CommunityAuthorization.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .email("john@mail.com")
            .name("John Doe")
            .maxSurfaceUnit(SQUARE_DEGREE)
            .apiKeys(new ArrayList<>())
            .build();
    int initialApiKeysSize = newCommunityAuthorization.getApiKeys().size();

    var actual = subject.generateApiKeys(List.of(newCommunityAuthorization));
    var retrievedCommunityAuthorization =
        communityAuthorizationRepository.findById(newCommunityAuthorization.getId()).get();

    assertEquals(
        retrievedCommunityAuthorization.getApiKeys().getLast().getKeyValue(),
        actual.getLast().apiKey());
    System.out.println(
        retrievedCommunityAuthorization.getApiKeys()
            + "\n"
            + newCommunityAuthorization.getApiKeys());
    assertTrue(retrievedCommunityAuthorization.getApiKeys().size() > initialApiKeysSize);
  }

  @Test
  void new_api_key_created_when_existing_community_used_on_generate_keys() {
    when(userAccountsApiMock.getOrGenerateApiKey(any(), any(), any()))
        .thenReturn(new UserApiKey(randomUUID().toString(), DASHBOARD));

    subject =
        new ApiKeyService(communityAuthorizationRepository, userAccountsApiMock, ADMIN_API_KEY);

    String existingCommunityAuthorizationId = randomUUID().toString();
    CommunityAuthorization existingCommunityAuthorization =
        CommunityAuthorization.builder()
            .id(existingCommunityAuthorizationId)
            .creationDatetime(now())
            .email("jane@mail.com")
            .name("Jane Doe")
            .maxSurfaceUnit(SQUARE_DEGREE)
            .dashboardApiKey(randomUUID().toString())
            .apiKeys(
                List.of(
                    CommunityAuthorizationApiKey.builder()
                        .communityOwnerId(existingCommunityAuthorizationId)
                        .creationDatetime(now())
                        .keyValue(randomUUID().toString())
                        .id(randomUUID().toString())
                        .build()))
            .build();
    communityAuthorizationRepository.save(existingCommunityAuthorization);

    var actual = subject.generateApiKeys(List.of(existingCommunityAuthorization));
    var retrievedCommunityAuthorization =
        communityAuthorizationRepository.findById(existingCommunityAuthorization.getId()).get();

    assertEquals(
        retrievedCommunityAuthorization.getMostRecentApiKey().getKeyValue(),
        actual.getLast().apiKey());
    assertTrue(
        retrievedCommunityAuthorization.getApiKeys().size()
            > existingCommunityAuthorization.getApiKeys().size());
  }
}
