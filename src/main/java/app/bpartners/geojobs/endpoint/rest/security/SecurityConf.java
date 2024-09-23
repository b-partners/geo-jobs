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
  private final HandlerExceptionResolver exceptionResolver;
  private final AuthenticationManager authenticationManager;

  public SecurityConf(
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      AuthenticationManager authenticationManager) {
    this.exceptionResolver = exceptionResolver;
    this.authenticationManager = authenticationManager;
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
            apiKeyAuthFilter(new NegatedRequestMatcher(anonymousPath)),
            AnonymousAuthenticationFilter.class)
        .authorizeHttpRequests(
            authorizationManagerRequestMatcherRegistry ->
                authorizationManagerRequestMatcherRegistry
                    .requestMatchers(anonymousPath)
                    .anonymous()
                    .requestMatchers("/jobs/*/annotationProcessing")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/tilingJobs", "/tilingJobs/**")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/tilingJobs/*/taskStatistics")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/tilingJobs")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/tilingJobs/import")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/tilingJobs/*/duplications")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/tilingJobs/*/taskFiltering")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/tilingJobs/*/retry")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/detectionJobs", "/detectionJobs/**")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/detectionJobs/*/taskStatistics")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/detectionJobs/**")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/detectionJobs/*/taskFiltering")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/detectionJobs/*/retry")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/detectionJobs/*/detectedParcels")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(POST, "/detectionJobs/*/humanVerificationStatus")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(GET, "/detectionJobs/*/geojsonsUrl")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/parcelization")
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/fullDetection")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/fullDetections")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
                    .requestMatchers(GET, "/usage")
                    .hasAnyAuthority(ROLE_ADMIN.name(), ROLE_COMMUNITY.name())
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

  private ApiKeyAuthFilter apiKeyAuthFilter(RequestMatcher requestMatcher) {
    ApiKeyAuthFilter apiKeyFilter = new ApiKeyAuthFilter(requestMatcher);
    apiKeyFilter.setAuthenticationManager(authenticationManager);
    apiKeyFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {});
    apiKeyFilter.setAuthenticationFailureHandler(
        (req, res, e) ->
            // note(spring-exception)
            // issues like when a user is not found(i.e. UsernameNotFoundException)
            // or other exceptions thrown inside authentication provider.
            // In fact, this handles other authentication exceptions that are
            // not handled by AccessDeniedException and AuthenticationEntryPoint
            exceptionResolver.resolveException(req, res, null, forbiddenWithRemoteInfo(e, req)));
    return apiKeyFilter;
  }
}
