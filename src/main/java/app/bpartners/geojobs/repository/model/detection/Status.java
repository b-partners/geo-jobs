package app.bpartners.geojobs.repository.model.detection;

import lombok.*;

public class Status {
  @Getter
  @AllArgsConstructor
  public enum Progression {
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    FINISHED("FINISHED");

    private final String value;
  }

  @Getter
  @AllArgsConstructor
  public enum Health {
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN"),
    RETRYING("RETRYING");

    private final String value;
  }
}
