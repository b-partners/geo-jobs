package app.bpartners.geojobs.repository.annotator.gen;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder
@Data
@EqualsAndHashCode
@ToString
public class TaskStatistics {
  private Integer remainingTasksForUserId;
  private Integer remainingTasks;
  private Integer completedTasksByUserId;
  private Integer totalTasks;
}
