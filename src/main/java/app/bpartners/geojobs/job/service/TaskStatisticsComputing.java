package app.bpartners.geojobs.job.service;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;

public class TaskStatisticsComputing<T extends Task>
    implements Function<List<T>, List<TaskStatusStatistic>> {

  @Override
  public List<TaskStatusStatistic> apply(List<T> tasks) {
    List<TaskStatusStatistic> taskStatusStatistics = new ArrayList<>();
    Stream<Status.ProgressionStatus> progressionStatuses =
        Arrays.stream(Status.ProgressionStatus.values());
    progressionStatuses.forEach(
        progressionStatus -> {
          var healthStatistics = new ArrayList<TaskStatusStatistic.HealthStatusStatistic>();
          Arrays.stream(Status.HealthStatus.values())
              .forEach(
                  healthStatus ->
                      healthStatistics.add(
                          computeHealthStatistics(tasks, progressionStatus, healthStatus)));
          taskStatusStatistics.add(
              TaskStatusStatistic.builder()
                  .progressionStatus(progressionStatus)
                  .healthStatusStatistics(healthStatistics)
                  .build());
        });
    return taskStatusStatistics;
  }

  @NonNull
  private TaskStatusStatistic.HealthStatusStatistic computeHealthStatistics(
      List<T> tasks, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return TaskStatusStatistic.HealthStatusStatistic.builder()
        .healthStatus(healthStatus)
        .count(
            tasks.stream()
                .filter(
                    task ->
                        task.getStatus().getProgression().equals(progressionStatus)
                            && task.getStatus().getHealth().equals(healthStatus))
                .count())
        .build();
  }
}
