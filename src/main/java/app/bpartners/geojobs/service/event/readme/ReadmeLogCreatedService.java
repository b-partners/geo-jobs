package app.bpartners.geojobs.service.event.readme;

import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.factory.ReadmeLogFactory;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.net.http.HttpClient.Version;
import static java.net.http.HttpResponse.BodyHandlers;
import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpClient.newHttpClient;

@Slf4j
@Service
public class ReadmeLogCreatedService implements Consumer<ReadmeLogCreated> {
  private static final String README_AUTH_PREFIX = "Basic ";
  private static final String README_API_MIME_TYPE = "application/json";
  private final ObjectMapper objectMapper;
  private final ReadmeLogFactory readmeLogFactory;
  private static final HttpClient httpClient = newHttpClient();

  public ReadmeLogCreatedService(ObjectMapper objectMapper, ReadmeLogFactory readmeLogFactory) {
    this.objectMapper = objectMapper;
    this.readmeLogFactory = readmeLogFactory;
  }

  @Override
  @SneakyThrows
  public void accept(ReadmeLogCreated readmeLogCreated) {
    var readmeMonitorConf = readmeLogCreated.getReadmeMonitorConf();
    var readmeLog =
        readmeLogFactory.createReadmeLog(
            readmeLogCreated.getRequest(),
            readmeLogCreated.getResponse(),
            readmeLogCreated.getStartedDatetime(),
            readmeLogCreated.getEndedDatetime(),
            readmeLogCreated.getPrincipal(),
            readmeMonitorConf);

    if (readmeMonitorConf.isDevelopment() != readmeLog.development()) {
      throw new BadRequestException(
          "readmeLog.development should be " + readmeMonitorConf.isDevelopment());
    }

    String requestBody = objectMapper.writeValueAsString(List.of(readmeLog));
    HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(readmeMonitorConf.getUrl()))
            .header("Content-Type", README_API_MIME_TYPE)
            .header("Authorization", getBasicAuthValue(readmeMonitorConf))
            .POST(BodyPublishers.ofString(requestBody))
            .build();
    HttpResponse<String> readmeResponse = httpClient.send(httpRequest, BodyHandlers.ofString());

    log.info("readme.monitor.requestBody: {}", requestBody);
    log.info("readme.monitor.responseBody: {}", readmeResponse.body());
    log.info("readme.monitor.responseStatus : {}", readmeResponse.statusCode());
  }

  private String getBasicAuthValue(ReadmeMonitorConf readmeMonitorConf) {
    String authInfo = readmeMonitorConf.getApiKey() + ":";
    return README_AUTH_PREFIX + Base64.getEncoder().encodeToString(authInfo.getBytes());
  }

  public static Version getClientVersion(){
    return httpClient.version();
  }
}
