package app.bpartners.geojobs.endpoint.rest.security.authorizer;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthentication;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.authorizer.CommunityZoneTilingJobProcessAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.model.CommunityAuthorizationDetails;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import app.bpartners.geojobs.repository.impl.CommunityAuthorizationDetailsRepositoryImpl;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class CommunityZoneTilingJobProcessAuthorizerIT extends FacadeIT {
  private final MockedStatic<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilter =
      mockStatic(ApiKeyAuthenticationFilter.class);

  @MockBean CommunityAuthorizationDetailsRepositoryImpl communityAuthorizationDetailsRepository;

  @Autowired CommunityZoneTilingJobProcessAuthorizer communityZoneTilingJobProcessAuthorizer;

  @Test
  void should_accept_directly_admin_key() {
    useRole(ROLE_ADMIN);
    assertDoesNotThrow(
        () -> {
          communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("dummy_zone_name"));
        });
  }

  @Test
  void should_throws_forbidden_if_zone_name_is_not_authorized() {
    useRole(ROLE_COMMUNITY);
    when(communityAuthorizationDetailsRepository.findByApiKey(any()))
        .thenReturn(asCommunityAuthorizationDetails(List.of("community_zone1", "community_zone2")));

    var error =
        assertThrows(
            ForbiddenException.class,
            () -> {
              communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("private_zone_name"));
            });
    assertTrue(error.getMessage().contains("private_zone_name"));
  }

  @Test
  void should_accept_if_zone_name_is_included_to_authorized_zone_name() {
    useRole(ROLE_COMMUNITY);
    when(communityAuthorizationDetailsRepository.findByApiKey(any()))
        .thenReturn(asCommunityAuthorizationDetails(List.of("community_zone1", "community_zone2")));

    assertDoesNotThrow(
        () -> {
          communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("community_zone1"));
        });
    assertDoesNotThrow(
        () -> {
          communityZoneTilingJobProcessAuthorizer.accept(asZoneTilingJob("community_zone2"));
        });
  }

  private CommunityAuthorizationDetails asCommunityAuthorizationDetails(
      List<String> detectableZoneName) {
    return new CommunityAuthorizationDetails(
        "dummy_id", "dummy_name", "dummy_name", detectableZoneName, List.of());
  }

  private ZoneTilingJob asZoneTilingJob(String zoneName) {
    var zoneTilingJob = new ZoneTilingJob();
    zoneTilingJob.setZoneName(zoneName);
    return zoneTilingJob;
  }

  private void useRole(Authority.Role role) {
    var apiKeyAuthentication =
        new ApiKeyAuthentication("dummy-api-key", Set.of(new Authority(role)));
    apiKeyAuthenticationFilter
        .when(ApiKeyAuthenticationFilter::getApiKeyAuthentication)
        .thenReturn(apiKeyAuthentication);
  }

  @AfterEach
  void cleanMock() {
    // to avoid creating multiple static mock registration
    apiKeyAuthenticationFilter.close();
  }
}
