package app.bpartners.geojobs.endpoint.rest.security.model;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;

@Getter
@ToString
@AllArgsConstructor
public class Principal implements UserDetails {
    private final String bearer;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return bearer;
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

    public boolean isAdmin() {
        return this.getAuthorities().contains(new Authority(ROLE_ADMIN));
    }
}
