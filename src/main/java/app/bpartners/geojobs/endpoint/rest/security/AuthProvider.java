package app.bpartners.geojobs.endpoint.rest.security;

import app.bpartners.geojobs.endpoint.rest.security.authenticator.UsernamePasswordAuthenticator;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthProvider extends AbstractUserDetailsAuthenticationProvider {
    private final UsernamePasswordAuthenticator authenticator;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken token) {
        // nothing
    }

    @Override
    protected UserDetails retrieveUser(
            String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
        return authenticator.retrieveUser(username, usernamePasswordAuthenticationToken);
    }


    public static Principal getPrincipal() {
        return (Principal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}