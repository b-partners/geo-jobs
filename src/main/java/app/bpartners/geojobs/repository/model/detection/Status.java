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

    public static Progression fromValue(String value) {
      for (Progression progression : Progression.values()) {
        if (progression.value.equalsIgnoreCase(value)) {
          return progression;
        }
      }
      throw new IllegalArgumentException("No enum constant Progression." + value);
    }
  }

  @Getter
  @AllArgsConstructor
  public enum Health {
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    UNKNOWN("UNKNOWN"),
    RETRYING("RETRYING");

    private final String value;

    public static Health fromValue(String value) {
      for (Health health : Health.values()) {
        if (health.value.equalsIgnoreCase(value)) {
          return health;
        }
      }
      throw new IllegalArgumentException("No enum constant Health." + value);
    }
  }
}
