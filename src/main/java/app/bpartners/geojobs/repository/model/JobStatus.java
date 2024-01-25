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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "zone_job_status")
@TypeDef(name = PGSQL_ENUM_NAME, typeClass = PostgresEnumType.class)
public class JobStatus extends Status {
  @JoinColumn(referencedColumnName = "id")
  private String jobId;

  @Enumerated(EnumType.STRING)
  @Type(type = PGSQL_ENUM_NAME)
  private JobType jobType;

  public static JobStatus from(String id, Status status, JobType jobType) {
    return JobStatus.builder()
        .jobId(id)
        .id(randomUUID().toString())
        .jobType(jobType)
        .progression(status.getProgression())
        .health(status.getHealth())
        .creationDatetime(status.getCreationDatetime())
        .build();
  }

  public enum JobType {
    TILING,
    DETECTION
  }
}
