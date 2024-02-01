package app.bpartners.geojobs.repository.model;

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
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode
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
    return sortedStatuses.stream().reduce(sortedStatuses.get(0), Status::reduce);
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
        case PENDING, PROCESSING -> throw new IllegalArgumentException(errorMessage);
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
}
