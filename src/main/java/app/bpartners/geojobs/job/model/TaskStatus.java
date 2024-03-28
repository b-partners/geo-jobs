package app.bpartners.geojobs.job.model;

import app.bpartners.geojobs.job.repository.JobTypeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;

@PrimaryKeyJoinColumn(name = "id")
@Entity
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "task_status")
public class TaskStatus extends Status implements Serializable {
  @JoinColumn private String taskId;

  @JsonIgnore // TODO(status.jobType-serialization)
  @Convert(converter = JobTypeConverter.class)
  private JobType jobType;

  public static TaskStatus from(String id, Status status, JobType jobType) {
    return TaskStatus.builder()
        .taskId(id)
        .id(status.getId())
        .jobType(jobType)
        .progression(status.getProgression())
        .health(status.getHealth())
        .message(status.getMessage())
        .creationDatetime(status.getCreationDatetime())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    TaskStatus that = (TaskStatus) o;
    return Objects.equals(taskId, that.taskId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), taskId);
  }
}
