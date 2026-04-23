package app.bpartners.geojobs.endpoint.event.model.status;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ParcelDetectionStatusRecomputingSubmitted extends JobStatusRecomputingSubmitted {
  private boolean isIntegrationTest;

  private static final long INITIAL_BACKOFF_IN_SECONDS = Duration.ofSeconds(30).toSeconds();

  public ParcelDetectionStatusRecomputingSubmitted(String parcelDetectionJobId) {
    super(parcelDetectionJobId, INITIAL_BACKOFF_IN_SECONDS);
  }

  public ParcelDetectionStatusRecomputingSubmitted(
      String parcelDetectionJobId, boolean isIntegrationTest) {
    super(parcelDetectionJobId, INITIAL_BACKOFF_IN_SECONDS);
    this.isIntegrationTest = isIntegrationTest;
  }
}
