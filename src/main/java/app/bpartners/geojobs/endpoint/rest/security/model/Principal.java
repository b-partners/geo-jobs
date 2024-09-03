package app.bpartners.geojobs.endpoint.rest.security.model;

import app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@ToString
@AllArgsConstructor
public class Principal implements UserDetails {
  private final String apiKey;
  private Collection<? extends GrantedAuthority> authorities;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return apiKey;
  }

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public boolean isAccountNonExpired() {
    return isEnabled();
  }

  @Override
  public boolean isAccountNonLocked() {
    return isEnabled();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return isEnabled();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public Role getRole() {
    if (authorities.size() != 1) {
      throw new RuntimeException("Only one role per principal expected but got: " + authorities);
    }

    return ((Authority) authorities.stream().toList().get(0)).value();
  }
}
