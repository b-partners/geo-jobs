package app.bpartners.readme.monitor.factory;

import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import app.bpartners.readme.monitor.ReadmeMonitorConf;
import app.bpartners.readme.monitor.model.ReadmeLog;
import app.bpartners.readme.monitor.model.ReadmeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadmeLogFactory {
  private final ReadmeGroupFactory readmeGroupFactory;
  private final ReadmeEntryFactory readmeEntryFactory;

  public ReadmeLog createReadmeLog(
      HttpServletRequest request,
      HttpServletResponse response,
      Instant startedDatetime,
      Instant endedDatetime,
      ReadmeMonitorConf readmeMonitorConf,
      Principal principal) {
    return ReadmeLog.builder()
        .clientIPAddress(request.getRemoteAddr())
        .development(readmeMonitorConf.isDevelopment())
        .group(readmeGroupFactory.createReadmeGroup(principal))
        .request(
            ReadmeRequest.builder()
                .log(
                    ReadmeRequest.ReadmeRequestLog.builder()
                        .entries(
                            List.of(
                                readmeEntryFactory.createReadmeEntry(
                                    request, response, startedDatetime, endedDatetime)))
                        .build())
                .build())
        .build();
  }
}
