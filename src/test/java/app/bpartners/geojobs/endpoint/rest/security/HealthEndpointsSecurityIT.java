package app.bpartners.geojobs.endpoint.rest.security;

import static app.bpartners.geojobs.endpoint.rest.security.authenticator.ApiKeyAuthenticator.API_KEY_HEADER;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_ADMIN;
import static app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role.ROLE_COMMUNITY;
import static java.net.http.HttpClient.newHttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.security.model.Authority.Role;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

class HealthEndpointsSecurityIT extends FacadeIT {
  private static final String ADMIN_KEY = "health-admin-key";
  private static final String REVOKED_ADMIN_KEY = "health-revoked-admin-key";
  private static final String COMMUNITY_KEY = "health-community-key";
  private static final String UNKNOWN_KEY = "health-unknown-key";

  @LocalServerPort private int port;
  @MockBean CommunityAuthorizationRepository caRepositoryMock;

  @BeforeEach
  void setUp() {
    when(caRepositoryMock.findByApiKey(ADMIN_KEY)).thenReturn(authorization(ROLE_ADMIN, false));
    when(caRepositoryMock.findByApiKey(REVOKED_ADMIN_KEY))
        .thenReturn(authorization(ROLE_ADMIN, true));
    when(caRepositoryMock.findByApiKey(COMMUNITY_KEY))
        .thenReturn(authorization(ROLE_COMMUNITY, false));
    when(caRepositoryMock.findByApiKey(UNKNOWN_KEY)).thenReturn(Optional.empty());
  }

  static Stream<Arguments> guardedEndpoints() {
    return Stream.of(
        Arguments.of("GET", "/health/db"),
        Arguments.of("GET", "/health/bucket"),
        Arguments.of("GET", "/health/event1"),
        Arguments.of("GET", "/health/event2"),
        Arguments.of("GET", "/health/email?to=someone@mail.com"),
        Arguments.of("GET", "/health/anything-else"),
        Arguments.of("POST", "/health/event/uuids"));
  }

  @ParameterizedTest(name = "anonymous {0} {1} is refused")
  @MethodSource("guardedEndpoints")
  void anonymous_is_refused(String method, String path) throws Exception {
    var response = call(method, path, null);

    assertEquals(403, response.statusCode());
    assertTrue(response.body().contains("Bad credentials"), response.body());
  }

  @ParameterizedTest(name = "unknown key on {0} {1} is refused")
  @MethodSource("guardedEndpoints")
  void unknown_key_is_refused(String method, String path) throws Exception {
    var response = call(method, path, UNKNOWN_KEY);

    assertEquals(403, response.statusCode());
    assertTrue(response.body().contains("Bad credentials"), response.body());
  }

  @ParameterizedTest(name = "revoked admin key on {0} {1} is refused")
  @MethodSource("guardedEndpoints")
  void revoked_admin_key_is_refused(String method, String path) throws Exception {
    var response = call(method, path, REVOKED_ADMIN_KEY);

    assertEquals(403, response.statusCode());
  }

  @ParameterizedTest(name = "community key on {0} {1} is refused")
  @MethodSource("guardedEndpoints")
  void community_key_is_refused(String method, String path) throws Exception {
    var response = call(method, path, COMMUNITY_KEY);

    assertEquals(403, response.statusCode());
    assertTrue(response.body().contains("Access Denied"), response.body());
  }

  @Test
  void admin_key_can_check_the_database() throws Exception {
    var response = call("GET", "/health/db", ADMIN_KEY);

    assertEquals(200, response.statusCode());
    assertEquals("OK", response.body());
  }

  @Test
  void admin_key_can_check_the_event_uuids() throws Exception {
    var response = call("POST", "/health/event/uuids", ADMIN_KEY);

    assertEquals(200, response.statusCode());
    assertEquals("OK", response.body());
  }

  @Test
  void ping_stays_anonymous() throws Exception {
    var response = call("GET", "/ping", null);

    assertEquals(200, response.statusCode());
    assertEquals("pong", response.body());
  }

  private HttpResponse<String> call(String method, String path, String apiKey) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Content-Type", "application/json");
    if (apiKey != null) {
      request.header(API_KEY_HEADER, apiKey);
    }
    request.method(
        method,
        "POST".equals(method)
            ? HttpRequest.BodyPublishers.ofString("[]")
            : HttpRequest.BodyPublishers.noBody());
    return newHttpClient().send(request.build(), BodyHandlers.ofString());
  }

  private static Optional<CommunityAuthorization> authorization(Role role, boolean revoked) {
    return Optional.of(
        CommunityAuthorization.builder().isApiKeyRevoked(revoked).role(role).build());
  }
}
