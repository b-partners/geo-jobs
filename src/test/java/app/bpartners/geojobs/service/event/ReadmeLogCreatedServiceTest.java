package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.readme.monitor.model.ReadmeLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadmeLogCreatedServiceTest {
  private static final String API_KEY = "readme-api-key";

  ObjectMapper objectMapperMock = mock();
  ReadmeLog readmeLogMock = mock();
  ReadmeLogCreatedService subject = new ReadmeLogCreatedService(objectMapperMock);
  HttpServer server;
  AtomicReference<String> receivedBody = new AtomicReference<>();
  AtomicReference<String> receivedAuthorization = new AtomicReference<>();
  AtomicReference<String> receivedContentType = new AtomicReference<>();
  AtomicReference<String> receivedMethod = new AtomicReference<>();

  @BeforeEach
  void setup() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/logs",
        exchange -> {
          receivedMethod.set(exchange.getRequestMethod());
          receivedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
          receivedBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          var response = "[]".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    server.start();
    when(readmeLogMock.development()).thenReturn(false);
    when(objectMapperMock.writeValueAsString(List.of(readmeLogMock)))
        .thenReturn("[{\"log\":true}]");
  }

  @AfterEach
  void cleanup() {
    server.stop(0);
    Thread.interrupted();
  }

  private ReadmeLogCreated event(String url, boolean development) {
    return ReadmeLogCreated.builder()
        .readmeMonitorConf(
            ReadmeMonitorConf.builder().apiKey(API_KEY).url(url).development(development).build())
        .readmeLog(readmeLogMock)
        .build();
  }

  private String serverUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort() + "/logs";
  }

  @Test
  void accept_posts_serialized_log_with_basic_auth_to_readme_url() {
    subject.accept(event(serverUrl(), false));

    assertEquals("POST", receivedMethod.get());
    assertEquals("[{\"log\":true}]", receivedBody.get());
    assertEquals("application/json", receivedContentType.get());
    assertEquals(
        "Basic " + Base64.getEncoder().encodeToString((API_KEY + ":").getBytes()),
        receivedAuthorization.get());
  }

  @Test
  void not_accept_if_development_is_different() {
    var event = event(serverUrl(), true);

    var error = assertThrows(BadRequestException.class, () -> subject.accept(event));

    assertEquals("readmeLog.development should be true", error.getMessage());
  }

  @Test
  void accept_fails_when_readme_is_unreachable() {
    server.stop(0);
    var event = event(serverUrl(), false);

    var error = assertThrows(RuntimeException.class, () -> subject.accept(event));

    assertEquals("Cannot save readmeLog", error.getMessage());
  }

  @Test
  void accept_fails_and_keeps_interrupted_flag_when_thread_is_interrupted() {
    var event = event(serverUrl(), false);
    Thread.currentThread().interrupt();

    var error = assertThrows(RuntimeException.class, () -> subject.accept(event));

    assertEquals("Cannot save readmeLog", error.getMessage());
    assertTrue(Thread.currentThread().isInterrupted());
  }

  @Test
  void client_version_is_available() {
    assertNotNull(ReadmeLogCreatedService.getClientVersion());
  }
}
