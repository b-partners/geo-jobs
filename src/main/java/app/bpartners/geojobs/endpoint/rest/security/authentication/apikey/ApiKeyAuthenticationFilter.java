package app.bpartners.geojobs.endpoint.rest.security.authentication.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

@AllArgsConstructor
public class ApiKeyAuthenticationFilter extends GenericFilterBean {

  private final RequestMatcher requestMatcher;
  private final ApiKeyAuthenticator apiKeyAuthenticator;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    var httpRequest = (HttpServletRequest) request;
    if (requestMatcher.matches(httpRequest)) {
      var authentication = apiKeyAuthenticator.apply(httpRequest);
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    chain.doFilter(request, response);
  }

  public static ApiKeyAuthentication getApiKeyAuthentication() {
    return (ApiKeyAuthentication) SecurityContextHolder.getContext().getAuthentication();
  }
}
