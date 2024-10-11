package app.bpartners.geojobs.endpoint.rest.readme.monitor.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeGroup;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.model.ReadmeLog;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

class ReadmeLogFactoryTest {
  private static final String REMOTE_ADDR = "192.162.15.1";
  ReadmeGroupFactory readmeGroupFactoryMock = mock();
  HttpServletRequest httpServletRequestMock = mock();
  ReadmeLogFactory subject = new ReadmeLogFactory(readmeGroupFactoryMock);

  @Test
  void can_create_readme_log() {
    var expectedReadmeGroup = mock(ReadmeGroup.class);
    var readmeMonitorConf = readmeMonitorConf();
    when(readmeGroupFactoryMock.createReadmeGroup(any())).thenReturn(expectedReadmeGroup);
    when(httpServletRequestMock.getRemoteAddr()).thenReturn(REMOTE_ADDR);

    var expected =
        ReadmeLog.builder()
            .group(expectedReadmeGroup)
            .clientIPAddress(REMOTE_ADDR)
            .development(readmeMonitorConf.isDevelopment())
            .build();

    var actual =
        subject.createReadmeLog(httpServletRequestMock, mock(Principal.class), readmeMonitorConf);

    assertEquals(expected, actual);
  }

  ReadmeMonitorConf readmeMonitorConf() {
    return ReadmeMonitorConf.builder()
        .name("readmeio")
        .version("0.0.1")
        .url("https://dummy.com")
        .apiKey("readme-admin-api")
        .development(true)
        .build();
  }
}
