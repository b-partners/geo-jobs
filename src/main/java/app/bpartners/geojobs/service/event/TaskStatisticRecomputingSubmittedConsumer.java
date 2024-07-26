package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.TaskStatisticFunction;
import app.bpartners.geojobs.job.service.TaskStatisticsComputing;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskStatisticRecomputingSubmittedConsumer<T extends Task, J extends Job>
    implements Consumer<TaskStatisticRecomputingSubmitted> {
  private final TaskStatisticFunction<T, J> taskStatisticFunction;
  private final JobRepository<J> jobRepository;
  private final TaskStatisticRepository taskStatisticRepository;

  public TaskStatisticRecomputingSubmittedConsumer(
      JobRepository<J> jobRepository,
      TaskRepository<T> taskRepository,
      TaskStatisticRepository taskStatisticRepository) {
    this.jobRepository = jobRepository;
    this.taskStatisticFunction =
        new TaskStatisticFunction<>(taskRepository, new TaskStatisticsComputing<>());
    this.taskStatisticRepository = taskStatisticRepository;
  }

  @Override
  public void accept(TaskStatisticRecomputingSubmitted taskStatisticRecomputingSubmitted) {
    String jobId = taskStatisticRecomputingSubmitted.getJobId();
    log.info("[DEBUG] TaskStatisticRecomputingSubmitted computing jobId={}", jobId);
    var job =
        jobRepository
            .findById(jobId)
            .orElseThrow(() -> new NotFoundException("job.id=" + jobId + " not found"));
    var taskStatistic = taskStatisticFunction.apply(job);
    log.info("[DEBUG] TaskStatistic to save {}", taskStatistic);
    var savedStatistic = taskStatisticRepository.save(taskStatistic);
    log.info("[DEBUG] TaskStatistic saved {}", savedStatistic);
  }
}
