package app.bpartners.geojobs.job.model.statistic;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.geojobs.job.model.Status;
import jakarta.persistence.*;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "\"task_status_statistic\"")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
public class TaskStatusStatistic {
  @Id private String id;

  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Status.ProgressionStatus progressionStatus;

  @OneToMany(cascade = CascadeType.ALL, fetch = EAGER, orphanRemoval = true)
  private List<HealthStatusStatistic> healthStatusStatistics;
}
