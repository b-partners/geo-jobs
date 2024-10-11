package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReadmeLogFactory {
  private final ReadmeGroupFactory readmeGroupFactory;

  public ReadmeLog createReadmeLog(
      HttpServletRequest request, Principal principal, ReadmeMonitorConf readmeMonitorConf) {
    return ReadmeLog.builder()
        .clientIPAddress(request.getRemoteAddr())
        .development(readmeMonitorConf.isDevelopment())
        .group(readmeGroupFactory.createReadmeGroup(principal))
        .build();
  }
}
