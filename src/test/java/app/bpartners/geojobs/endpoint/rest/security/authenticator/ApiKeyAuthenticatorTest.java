package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityUserSubscriptionAuthorizer;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.community.RevokedApiKey;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class ApiKeyAuthenticatorTest {
  private static final String KEY = "key";
  private static final String OTHER_KEY = "other-key";
  private static final String DELEGATION_KEY = "delegation-key";

  private final CommunityAuthorizationRepository repository =
      mock(CommunityAuthorizationRepository.class);
  private final ApiKeyAuthenticator subject =
      new ApiKeyAuthenticator(
          repository, mock(CommunityUserSubscriptionAuthorizer.class), DELEGATION_KEY);

  private CommunityAuthorization community(List<RevokedApiKey> revokedApiKeys) {
    return CommunityAuthorization.builder()
        .id("community_id")
        .role(ROLE_COMMUNITY)
        .revokedApiKeys(revokedApiKeys)
        .build();
  }

  private void authenticate(String apiKey) {
    subject.retrieveUser(
        API_KEY_HEADER, new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey));
  }

  @Test
  void active_key_is_accepted() {
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of())));

    assertDoesNotThrow(() -> authenticate(KEY));
  }

  @Test
  void revoked_key_is_refused() {
    var revoked = RevokedApiKey.builder().revokedApiKeyValue(KEY).build();
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of(revoked))));

    assertThrows(BadCredentialsException.class, () -> authenticate(KEY));
  }

  @Test
  void other_key_of_the_same_community_stays_valid_when_one_is_revoked() {
    var revoked = RevokedApiKey.builder().revokedApiKeyValue(OTHER_KEY).build();
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of(revoked))));

    assertDoesNotThrow(() -> authenticate(KEY));
  }

  private void delegate(String apiKey, String user) {
    var token = new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey);
    token.setDetails(user);
    subject.retrieveUser(API_KEY_HEADER, token);
  }

  private CommunityAuthorization delegatedCommunity() {
    return CommunityAuthorization.builder()
        .id("community_id")
        .role(ROLE_COMMUNITY)
        .apiKey(KEY)
        .dashboardUserId("user-1")
        .build();
  }

  @Test
  void delegation_key_runs_as_the_named_users_community_with_its_own_key() {
    when(repository.findByDashboardUserId("user-1")).thenReturn(Optional.of(delegatedCommunity()));

    var principal =
        (app.bpartners.geojobs.endpoint.rest.security.model.Principal)
            subject.retrieveUser(API_KEY_HEADER, tokenWithDetails(DELEGATION_KEY, "user-1"));

    org.junit.jupiter.api.Assertions.assertEquals(KEY, principal.getApiKey());
    assertDoesNotThrow(() -> delegate(DELEGATION_KEY, "user-1"));
  }

  private UsernamePasswordAuthenticationToken tokenWithDetails(String apiKey, String user) {
    var token = new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey);
    token.setDetails(user);
    return token;
  }

  @Test
  void delegation_refuses_unknown_user_and_revoked_community() {
    when(repository.findByDashboardUserId("ghost")).thenReturn(Optional.empty());
    var revoked = delegatedCommunity();
    revoked.setApiKeyRevoked(true);
    when(repository.findByDashboardUserId("user-2")).thenReturn(Optional.of(revoked));

    assertThrows(BadCredentialsException.class, () -> delegate(DELEGATION_KEY, "ghost"));
    assertThrows(BadCredentialsException.class, () -> delegate(DELEGATION_KEY, "user-2"));
  }

  @Test
  void on_behalf_of_is_ignored_without_the_delegation_key() {
    when(repository.findByDashboardUserId("user-1")).thenReturn(Optional.of(delegatedCommunity()));
    when(repository.findByApiKey(KEY)).thenReturn(Optional.of(community(List.of())));

    // a normal community key naming someone else still authenticates as itself only
    assertDoesNotThrow(() -> delegate(KEY, "someone-else"));
    // an unknown key naming a user gets nothing
    assertThrows(BadCredentialsException.class, () -> delegate("guess", "user-1"));
  }
}
