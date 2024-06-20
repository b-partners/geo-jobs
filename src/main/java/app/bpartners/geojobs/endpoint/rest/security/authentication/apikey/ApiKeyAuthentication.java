package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {
  private final String apiKey;

  public ApiKeyAuthentication(String apiKey, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.apiKey = apiKey;
    setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return apiKey;
  }

  public boolean isAdmin() {
    return this.getAuthorities().contains(new Authority(ROLE_ADMIN));
  }

  public String getApiKey() {
    return (String) this.getPrincipal();
  }
}
