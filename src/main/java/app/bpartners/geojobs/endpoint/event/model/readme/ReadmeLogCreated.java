package app.bpartners.geojobs.endpoint.event.model.readme;

import app.bpartners.geojobs.endpoint.event.model.PojaEvent;
import app.bpartners.geojobs.endpoint.rest.readme.monitor.ReadmeMonitorConf;
import app.bpartners.geojobs.endpoint.rest.security.model.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.*;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
public class ReadmeLogCreated extends PojaEvent {
  private HttpServletRequest request;
  private HttpServletResponse response;
  private Instant startedDatetime;
  private Instant endedDatetime;
  private Principal principal;
  private ReadmeMonitorConf readmeMonitorConf;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
