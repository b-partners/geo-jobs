package app.bpartners.geojobs.job.service;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.TaskRepository;
import java.util.function.Function;

public class TaskStatisticFunction<T extends Task, J extends Job>
    implements Function<J, TaskStatistic> {
  private final TaskRepository<T> taskRepository;
  private final TaskStatisticsComputing<T> taskStatisticsComputing;

  public TaskStatisticFunction(
      TaskRepository<T> taskRepository, TaskStatisticsComputing<T> taskStatisticsComputing) {
    this.taskRepository = taskRepository;
    this.taskStatisticsComputing = taskStatisticsComputing;
  }

  @Override
  public TaskStatistic apply(J job) {
    var tasks = taskRepository.findAllByJobId(job.getId());
    var taskStatusStatistics = taskStatisticsComputing.apply(tasks);
    return TaskStatistic.builder()
        .jobId(job.getId())
        .jobType(job.getStatus().getJobType())
        .actualJobStatus(job.getStatus())
        .taskStatusStatistics(taskStatusStatistics)
        .updatedAt(job.getStatus().getCreationDatetime())
        .build();
  }
}
