package app.bpartners.geojobs.service.event.readme;

import app.bpartners.geojobs.endpoint.event.model.readme.ReadmeLogCreated;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.factory.ReadmeLogFactory;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadmeLogCreatedService implements Consumer<ReadmeLogCreated> {
  private static final String README_AUTH_PREFIX = "Basic ";
  private static final String README_API_MIME_TYPE = "application/json";
  private static final OkHttpClient README_CLIENT = new OkHttpClient();
  private final ObjectMapper objectMapper;
  private final ReadmeLogFactory readmeLogFactory;
  private final AuthProvider authProvider;
  private final ReadmeMonitorConf readmeMonitorConf;

  @Override
  @SneakyThrows
  public void accept(ReadmeLogCreated readmeLogCreated) {
    var readmeLog =
        readmeLogFactory.createReadmeLog(
            readmeLogCreated.getRequest(),
            readmeLogCreated.getResponse(),
            readmeLogCreated.getStartedDatetime(),
            readmeLogCreated.getEndedDatetime(),
            authProvider.getPrincipal(),
            readmeMonitorConf);

    if (readmeMonitorConf.isDevelopment() != readmeLog.development()) {
      throw new BadRequestException(
          "readmeLog.development should be " + readmeMonitorConf.isDevelopment());
    }

    MediaType mediaType = MediaType.parse(README_API_MIME_TYPE);
    RequestBody requestBody =
        RequestBody.create(mediaType, objectMapper.writeValueAsString(List.of(readmeLog)));
    Request readmeRequest =
        new Request.Builder()
            .url(readmeMonitorConf.getUrl())
            .header("Content-Type", README_API_MIME_TYPE)
            .header("Authorization", getBasicAuthValue())
            .post(requestBody)
            .build();
    Response readmeResponse = README_CLIENT.newCall(readmeRequest).execute();

    log.info("readme.monitor.requestBody: {}", requestBody);
    log.info("readme.monitor.responseBody: {}", readmeResponse.body().string());
    log.info("readme.monitor.responseStatus : {}", readmeResponse.code());
  }

  private String getBasicAuthValue() {
    String authInfo = readmeMonitorConf.getApiKey() + ":";
    return README_AUTH_PREFIX + Base64.getEncoder().encodeToString(authInfo.getBytes());
  }
}
