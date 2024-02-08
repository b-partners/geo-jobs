package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Status {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private ProgressionStatus progression;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private HealthStatus health;

  @CreationTimestamp private Instant creationDatetime;
  private String message;

  public static Status reduce(List<Status> statuses) {
    var sortedStatuses =
        statuses.stream().sorted(comparing(Status::getCreationDatetime, naturalOrder())).toList();
    return sortedStatuses.stream()
        // TODO(invalid-history)? may be should give no default value like this...
        .reduce(Status.builder().progression(PENDING).health(UNKNOWN).build(), Status::reduce);
  }

  private static Status reduce(Status oldStatus, Status newStatus) {
    var errorMessage =
        String.format("Illegal status transition: old=%s, new=%s", oldStatus, newStatus);

    var oldProgression = oldStatus.getProgression();
    var newProgression = newStatus.getProgression();
    ProgressionStatus reducedProgression = reduce(oldProgression, newProgression, errorMessage);

    var oldHealth = oldStatus.getHealth();
    var newHealth = newStatus.getHealth();
    HealthStatus reducedHealth = reduce(oldHealth, newHealth, errorMessage);

    return oldProgression.equals(reducedProgression) && oldHealth.equals(reducedHealth)
        ? oldStatus
        : newStatus;
  }

  private static ProgressionStatus reduce(
      ProgressionStatus oldProgression, ProgressionStatus newProgression, String errorMessage) {
    return switch (oldProgression) {
      case PENDING -> newProgression;
      case PROCESSING -> switch (newProgression) {
        case PENDING -> throw new IllegalArgumentException(errorMessage);
        case PROCESSING, FINISHED -> newProgression;
      };
      case FINISHED -> switch (newProgression) {
        case PENDING, PROCESSING -> throw new IllegalArgumentException(
            errorMessage); // TODO(invalid-history): test using detected invalid history
        case FINISHED -> newProgression;
      };
    };
  }

  private static HealthStatus reduce(
      HealthStatus oldHealth, HealthStatus newHealth, String errorMessage) {
    return switch (oldHealth) {
      case UNKNOWN -> newHealth;
      case SUCCEEDED -> switch (newHealth) {
        case SUCCEEDED -> newHealth;
        case UNKNOWN, FAILED -> throw new IllegalArgumentException(errorMessage);
      };
      case FAILED -> switch (newHealth) {
        case FAILED -> newHealth;
        case UNKNOWN, SUCCEEDED -> throw new IllegalArgumentException(errorMessage);
      };
    };
  }

  public enum ProgressionStatus {
    PENDING,
    PROCESSING,
    FINISHED
  }

  public enum HealthStatus {
    UNKNOWN,
    SUCCEEDED,
    FAILED
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Status status = (Status) o;
    // /!\ Do NOT include id, as we want status with same (progression,health,time,message)
    // but with different id to still be equal
    return progression == status.progression
        && health == status.health
        && Objects.equals(creationDatetime, status.creationDatetime)
        && Objects.equals(message, status.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(progression, health, creationDatetime, message);
  }
}
