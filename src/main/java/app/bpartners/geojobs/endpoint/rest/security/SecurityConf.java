package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import app.bpartners.geojobs.model.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@Slf4j
@EnableWebSecurity
public class SecurityConf {
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;

  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    this.authProvider = authProvider;
    this.exceptionResolver = exceptionResolver;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    var anonymousPath =
        new OrRequestMatcher(
            new AntPathRequestMatcher("/**", OPTIONS.toString()),
            new AntPathRequestMatcher("/ping", GET.name()),
            new AntPathRequestMatcher("/health/event/uuids", POST.name()),
            new AntPathRequestMatcher("/health/**", GET.name()));
    httpSecurity
        .exceptionHandling(
            (exceptionHandler) ->
                exceptionHandler
                    .authenticationEntryPoint(
                        // note(spring-exception)
                        // https://stackoverflow.com/questions/59417122/how-to-handle-usernamenotfoundexception-spring-security
                        // issues like when a user tries to access a resource
                        // without appropriate authentication elements
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(e, req)))
                    .accessDeniedHandler(
                        // note(spring-exception): issues like when a user not having required roles
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(e, req))))
        .addFilterBefore(
            bearerFilter(new NegatedRequestMatcher(anonymousPath)),
            AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            authorizationManagerRequestMatcherRegistry ->
                authorizationManagerRequestMatcherRegistry
                    .requestMatchers(anonymousPath)
                    .anonymous()
                    .requestMatchers("/jobs/*/annotationProcessing")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/tilingJobs", "/tilingJobs/**")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/tilingJobs/*/taskStatistics")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/tilingJobs")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/tilingJobs/import")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/tilingJobs/*/duplications")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/tilingJobs/*/taskFiltering")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(PUT, "/tilingJobs/*/retry")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/detectionJobs", "/detectionJobs/**")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/detectionJobs/*/taskStatistics")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/detectionJobs/**")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(PUT, "/detectionJobs/*/taskFiltering")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(PUT, "/detectionJobs/*/retry")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/detectionJobs/*/detectedParcels")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(POST, "/detectionJobs/*/humanVerificationStatus")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/detectionJobs/*/geojsonsUrl")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(PUT, "/parcelization")
                    .hasAuthority(ROLE_ADMIN.name())
                    .anyRequest()
                    .denyAll())
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(Customizer.withDefaults())
        .sessionManagement(
            httpSecuritySessionManagementConfigurer ->
                httpSecuritySessionManagementConfigurer.sessionCreationPolicy(STATELESS));

    return httpSecurity.build();
  }

  private Exception forbiddenWithRemoteInfo(Exception e, HttpServletRequest req) {
    log.info(
        String.format(
            "Access is denied for remote caller: address=%s, host=%s, port=%s",
            req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException(e.getMessage());
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authProvider);
  }

  private BearerAuthFilter bearerFilter(RequestMatcher requestMatcher) {
    BearerAuthFilter bearerFilter = new BearerAuthFilter(requestMatcher);
    bearerFilter.setAuthenticationManager(authenticationManager());
    bearerFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {});
    bearerFilter.setAuthenticationFailureHandler(
        (req, res, e) ->
            // note(spring-exception)
            // issues like when a user is not found(i.e. UsernameNotFoundException)
            // or other exceptions thrown inside authentication provider.
            // In fact, this handles other authentication exceptions that are
            // not handled by AccessDeniedException and AuthenticationEntryPoint
            exceptionResolver.resolveException(req, res, null, forbiddenWithRemoteInfo(e, req)));
    return bearerFilter;
  }
}
