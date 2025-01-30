package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.HashSet;
import java.util.Objects;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator implements UsernamePasswordAuthenticator {
  public static final String API_KEY_HEADER = "x-api-key";
  @Getter private final String adminApiKey;
  private final CommunityAuthorizationRepository caRepository;

  public ApiKeyAuthenticator(
      @Value("${admin.api.key}") String adminApiKey,
      CommunityAuthorizationRepository communityAuthorizationRepository) {
    this.adminApiKey = adminApiKey;
    this.caRepository = communityAuthorizationRepository;
  }

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String candidateApiKey = getApiKeyFromHeader(usernamePasswordAuthenticationToken);
    HashSet<Authority> authorities = getAuthorities(candidateApiKey);
    if (!authorities.isEmpty()) {
      return new Principal(candidateApiKey, authorities);
    }
    throw new BadCredentialsException("Bad credentials");
  }

  private HashSet<Authority> getAuthorities(String candidateApiKey) {
    HashSet<Authority> authorities = new HashSet<>();
    if (adminApiKey.equals(candidateApiKey)) {
      authorities.add(new Authority(ROLE_ADMIN));
    }
    if (existsAsNotRevokedApiKeyInCommunityKeys(candidateApiKey)) {
      authorities.add(new Authority(ROLE_COMMUNITY));
    }
    return authorities;
  }

  private boolean existsAsNotRevokedApiKeyInCommunityKeys(String candidateApiKey) {
    var communityAuthorization = caRepository.findByApiKey(candidateApiKey);
    return communityAuthorization
        .filter(authorization -> !authorization.isApiKeyRevoked())
        .isPresent();
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
