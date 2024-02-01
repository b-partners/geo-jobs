package app.bpartners.geojobs.repository.model;

import static jakarta.persistence.EnumType.STRING;
import static java.util.UUID.randomUUID;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.repository.model.geo.JobType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "task_status")
public class TaskStatus extends Status {
  @JoinColumn private String taskId;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private JobType jobType;

  public static TaskStatus from(String id, Status status, JobType jobType) {
    return TaskStatus.builder()
        .taskId(id)
        .id(randomUUID().toString())
        .jobType(jobType)
        .progression(status.getProgression())
        .health(status.getHealth())
        .creationDatetime(status.getCreationDatetime())
        .build();
  }
}
