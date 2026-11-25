package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bpartners.geojobs.endpoint.rest.security.authorizer.CommunityUserSubscriptionAuthorizer;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ApiKeyAuthenticator implements UsernamePasswordAuthenticator {
  public static final String API_KEY_HEADER = "x-api-key";
  public static final String ON_BEHALF_OF_HEADER = "x-on-behalf-of";
  private final CommunityAuthorizationRepository caRepository;
  private final CommunityUserSubscriptionAuthorizer communityUserSubscriptionAuthorizer;
  private final String delegationApiKey;

  public ApiKeyAuthenticator(
      CommunityAuthorizationRepository communityAuthorizationRepository,
      CommunityUserSubscriptionAuthorizer communityUserSubscriptionAuthorizer,
      @Value("${delegation.api.key:}") String delegationApiKey) {
    this.caRepository = communityAuthorizationRepository;
    this.communityUserSubscriptionAuthorizer = communityUserSubscriptionAuthorizer;
    this.delegationApiKey = delegationApiKey;
  }

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String candidateApiKey = getApiKeyFromHeader(usernamePasswordAuthenticationToken);
    if (isDelegation(candidateApiKey, usernamePasswordAuthenticationToken.getDetails())) {
      return retrieveDelegatedUser((String) usernamePasswordAuthenticationToken.getDetails());
    }
    HashSet<Authority> authorities = getAuthorities(candidateApiKey);
    if (!authorities.isEmpty()) {
      return new Principal(candidateApiKey, authorities);
    }
    throw new BadCredentialsException("Bad credentials");
  }

  /**
   * A trusted caller (analysis-api) holds {@code delegation.api.key} and names the dashboard user
   * it acts for. The request then runs as that user's community with one of its own keys, so every
   * downstream check (surface quota, zones, models, ownership) applies unchanged.
   */
  private boolean isDelegation(String candidateApiKey, Object onBehalfOf) {
    return StringUtils.hasText(delegationApiKey)
        && candidateApiKey != null
        && onBehalfOf instanceof String user
        && StringUtils.hasText(user)
        && MessageDigest.isEqual(delegationApiKey.getBytes(UTF_8), candidateApiKey.getBytes(UTF_8));
  }

  private UserDetails retrieveDelegatedUser(String dashboardUserId) {
    var community =
        caRepository
            .findByDashboardUserId(dashboardUserId)
            .filter(authorization -> !authorization.isApiKeyRevoked())
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
    var key = community.getApiKey();
    if (key == null || isRevoked(community, key)) {
      throw new BadCredentialsException("Bad credentials");
    }
    return new Principal(key, new HashSet<>(Set.of(new Authority(community.getRole()))));
  }

  private HashSet<Authority> getAuthorities(String candidateApiKey) {
    HashSet<Authority> authorities = new HashSet<>();
    var authenticatedCommunity =
        caRepository
            .findByApiKey(candidateApiKey)
            .filter(authorization -> !authorization.isApiKeyRevoked())
            .filter(authorization -> !isRevoked(authorization, candidateApiKey))
            .orElseThrow(() -> new BadCredentialsException("Bad credentials"));
    // communityUserSubscriptionAuthorizer.accept(authenticatedCommunity); TODO: uncomment
    authorities.add(new Authority(authenticatedCommunity.getRole()));
    return authorities;
  }

  private static boolean isRevoked(CommunityAuthorization authorization, String apiKey) {
    var revokedApiKeys = authorization.getRevokedApiKeys();
    return revokedApiKeys != null
        && revokedApiKeys.stream()
            .anyMatch(revoked -> apiKey.equals(revoked.getRevokedApiKeyValue()));
  }

  private String getApiKeyFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String)
        || !Objects.equals(usernamePasswordAuthenticationToken.getName(), API_KEY_HEADER)) {
      return null;
    }
    return ((String) tokenObject);
  }
}
