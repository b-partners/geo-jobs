package app.bpartners.geojobs.endpoint.event.model.status;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ZDJStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private boolean isIntegrationTest;
  private static final long INITIAL_BACKOFF_IN_SECONDS = Duration.ofSeconds(30).toSeconds();

  public ZDJStatusRecomputingSubmitted(String jobId) {
    super(jobId, INITIAL_BACKOFF_IN_SECONDS);
    this.isIntegrationTest = false;
  }

  public ZDJStatusRecomputingSubmitted(String jobId, boolean isIntegrationTest) {
    super(jobId, INITIAL_BACKOFF_IN_SECONDS);
    this.isIntegrationTest = isIntegrationTest;
  }
}
