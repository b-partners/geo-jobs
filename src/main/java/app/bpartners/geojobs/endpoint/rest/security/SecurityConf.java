package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticationFilter;
import app.bpartners.geojobs.endpoint.rest.security.authentication.apikey.ApiKeyAuthenticator;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class SecurityConf {

  private final ApiKeyAuthenticator apiKeyAuthenticator;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
    var anonymousPath =
        new OrRequestMatcher(
            new AntPathRequestMatcher("/**", OPTIONS.toString()),
            new AntPathRequestMatcher("/ping", GET.name()),
            new AntPathRequestMatcher("/health/event/uuids", POST.name()),
            new AntPathRequestMatcher("/health/**", GET.name()));
    httpSecurity
        .addFilterBefore(
            new ApiKeyAuthenticationFilter(
                new NegatedRequestMatcher(anonymousPath), apiKeyAuthenticator),
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
                    .hasAuthority(ROLE_ADMIN.name())
                    .requestMatchers(PUT, "/parcelization")
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
}