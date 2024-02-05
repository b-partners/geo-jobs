package app.bpartners.geojobs.endpoint.rest.security.model;

import org.springframework.security.core.GrantedAuthority;

public record Authority(Role value) implements GrantedAuthority {

  public enum Role {
    ROLE_ADMIN
  }

  @Override
  public String getAuthority() {
    return value().name();
  }
}
