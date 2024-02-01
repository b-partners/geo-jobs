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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "job_status")
public class JobStatus extends Status {
  @JoinColumn(referencedColumnName = "id")
  private String jobId;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
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
}
