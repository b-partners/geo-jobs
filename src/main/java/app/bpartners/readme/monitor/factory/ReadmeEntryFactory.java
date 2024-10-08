package app.bpartners.readme.monitor.factory;

import static java.net.http.HttpClient.Version.HTTP_2;

import app.bpartners.readme.monitor.model.entry.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReadmeEntryFactory {
  public ReadmeEntry createReadmeEntry(
      HttpServletRequest request,
      HttpServletResponse response,
      Instant startedDatetime,
      Instant endedDatetime) {
    return ReadmeEntry.builder()
        .pareRef(request.getRequestURI())
        .startedDateTime(startedDatetime)
        .time(Duration.between(startedDatetime, endedDatetime).toMillis())
        .request(
            ReadmeEntryRequest.builder()
                .method(request.getMethod())
                .httpVersion(HTTP_2.toString())
                .url(request.getRequestURL().toString())
                .headers(retrieveHeaders(request))
                .queryString(retrieveQueries(request))
                .build())
        .response(
            ReadmeEntryResponse.builder()
                .status(response.getStatus())
                .headers(retrieveHeaders(response))
                .statusText(HttpStatus.valueOf(response.getStatus()).name())
                .content(
                    ReadmeEntryResponseContent.builder()
                        .mimeType(response.getContentType())
                        .build())
                .build())
        .build();
  }

  private List<ReadmeEntryHeader> retrieveHeaders(HttpServletResponse response) {
    return response.getHeaderNames().stream()
        .map(
            headerName ->
                ReadmeEntryHeader.builder()
                    .name(headerName)
                    .value(response.getHeader(headerName))
                    .build())
        .toList();
  }

  private List<ReadmeEntryHeader> retrieveHeaders(HttpServletRequest request) {
    var headerNames = request.getHeaderNames();
    var headers = new ArrayList<ReadmeEntryHeader>();

    while (headerNames.hasMoreElements()) {
      var headerName = headerNames.nextElement();
      headers.add(
          ReadmeEntryHeader.builder()
              .name(headerName)
              .value(request.getHeader(headerName))
              .build());
    }
    return headers;
  }

  private List<ReadmeEntryQuery> retrieveQueries(HttpServletRequest request) {
    var parameterNames = request.getParameterNames();
    var parameters = new ArrayList<ReadmeEntryQuery>();

    while (parameterNames.hasMoreElements()) {
      var parameterName = parameterNames.nextElement();
      parameters.add(
          ReadmeEntryQuery.builder()
              .name(parameterName)
              .value(request.getParameter(parameterName))
              .build());
    }
    return parameters;
  }
}
