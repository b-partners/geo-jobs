package app.bpartners.geojobs.job.model.statistic;

import app.bpartners.geojobs.job.model.Status;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
public class TaskStatusStatistic {
  private Status.ProgressionStatus progressionStatus;
  private List<HealthStatusStatistic> healthStatusStatistics;

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder(toBuilder = true)
  @Data
  @ToString
  public static class HealthStatusStatistic {
    private Status.HealthStatus healthStatus;
    private long count;
  }
}
