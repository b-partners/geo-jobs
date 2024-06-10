package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static app.bpartners.geojobs.repository.impl.CommunityAuthorizationDetailsRepositoryImpl.COMMUNITY_AUTHORIZATION_DETAILS_ENV_KEY;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import app.bpartners.geojobs.repository.CommunityAuthorizationDetailsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator implements Function<HttpServletRequest, Authentication> {
  public static final String APIKEY_HEADER_NAME = "x-api-key";
  private final String adminApiKey;
  private final CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository;

  public ApiKeyAuthenticator(
      @Value("${admin.api.key}") String adminApiKey,
      @Value(COMMUNITY_AUTHORIZATION_DETAILS_ENV_KEY) String communityAuthorizationDetails,
      CommunityAuthorizationDetailsRepository communityAuthorizationDetailsRepository) {
    this.adminApiKey = adminApiKey;
    this.communityAuthorizationDetailsRepository = communityAuthorizationDetailsRepository;
  }

  @Override
  public Authentication apply(HttpServletRequest request) {
    String candidateApiKey = request.getHeader(APIKEY_HEADER_NAME);
    if (existsAsApiKeyInCommunityKeys(candidateApiKey)) {
      return new ApiKeyAuthentication(candidateApiKey, Set.of(new Authority(ROLE_COMMUNITY)));
    } else if (adminApiKey.equals(candidateApiKey)) {
      return new ApiKeyAuthentication(candidateApiKey, Set.of(new Authority(ROLE_ADMIN)));
    }
    throw new BadCredentialsException("Invalid API Key");
  }

  private boolean existsAsApiKeyInCommunityKeys(String candidateApiKey) {
    return communityAuthorizationDetailsRepository.existsByApiKey(candidateApiKey);
  }
}
