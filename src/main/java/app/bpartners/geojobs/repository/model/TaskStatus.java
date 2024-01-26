package app.bpartners.geojobs.repository.model;

import static app.bpartners.geojobs.repository.model.types.PostgresEnumType.PGSQL_ENUM_NAME;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.types.PostgresEnumType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "zone_task_status")
@TypeDef(name = PGSQL_ENUM_NAME, typeClass = PostgresEnumType.class)
public class TaskStatus extends Status {
  @JoinColumn private String taskId;

  @Enumerated(EnumType.STRING)
  @Type(type = PGSQL_ENUM_NAME)
  private JobStatus.JobType jobType;

  public static TaskStatus from(String id, Status status, JobStatus.JobType jobType) {
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
