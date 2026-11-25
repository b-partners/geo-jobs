package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.ON_BEHALF_OF_HEADER;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Slf4j
public class ApiKeyAuthFilter extends AbstractAuthenticationProcessingFilter {
  protected ApiKeyAuthFilter(RequestMatcher requestMatcher) {
    super(requestMatcher);
  }

  @Override
  public Authentication attemptAuthentication(
      HttpServletRequest request, HttpServletResponse response) {
    String apiKey = request.getHeader(API_KEY_HEADER);
    AuthenticationManager manager = this.getAuthenticationManager();
    var token = new UsernamePasswordAuthenticationToken(API_KEY_HEADER, apiKey);
    token.setDetails(request.getHeader(ON_BEHALF_OF_HEADER));
    return manager.authenticate(token);
  }

  @Override
  protected void successfulAuthentication(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain,
      Authentication authenticated)
      throws IOException, ServletException {
    super.successfulAuthentication(request, response, chain, authenticated);
    chain.doFilter(request, response);
  }
}
