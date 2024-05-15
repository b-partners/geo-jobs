package app.bpartners.geojobs.job.model.statistic;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.JobType;
import java.time.Instant;
import java.util.List;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@ToString
public class TaskStatistic {
  private String jobId;
  private JobType jobType;
  private Instant updatedAt;
  private JobStatus actualJobStatus;
  private List<TaskStatusStatistic> taskStatusStatistics;
}
