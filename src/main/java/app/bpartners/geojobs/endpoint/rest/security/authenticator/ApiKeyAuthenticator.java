package app.bpartners.geojobs.endpoint.rest.security.authenticator;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator implements UsernamePasswordAuthenticator {
  public static final String API_KEY_HEADER = "x-api-key";
  private final String adminApiKey;
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
    if (existsAsNotRevokedApiKeyInCommunityKeys(candidateApiKey)) {
      return new Principal(candidateApiKey, Set.of(new Authority(ROLE_COMMUNITY)));
    } else if (adminApiKey.equals(candidateApiKey)) {
      return new Principal(candidateApiKey, Set.of(new Authority(ROLE_ADMIN)));
    }
    throw new BadCredentialsException("Bad credentials");
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
