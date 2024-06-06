package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;

import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.model.statistic.TaskStatusStatistic;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.ZDJTaskStatisticMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TaskStatisticRecomputingSubmittedService
    implements Consumer<TaskStatisticRecomputingSubmitted> {
  private final ZoneDetectionJobService detectionJobService;
  private final DetectionTaskRepository detectionTaskRepository;
  private final ZDJTaskStatisticMailer taskStatisticMailer;

  @Override
  public void accept(TaskStatisticRecomputingSubmitted taskStatisticRecomputingSubmitted) {
    String jobId = taskStatisticRecomputingSubmitted.getJobId();
    ZoneDetectionJob job = detectionJobService.findById(jobId);
    List<DetectionTask> detectionTasks = detectionTaskRepository.findAllByJobId(jobId);

    List<TaskStatusStatistic> taskStatusStatistics = getTaskStatusStatistics(detectionTasks);
    TaskStatistic taskStatistic =
        TaskStatistic.builder()
            .jobId(jobId)
            .jobType(DETECTION)
            .actualJobStatus(job.getStatus())
            .taskStatusStatistics(taskStatusStatistics)
            .updatedAt(job.getStatus().getCreationDatetime())
            .build();

    taskStatisticMailer.accept(taskStatistic, job);
  }

  @NonNull
  private List<TaskStatusStatistic> getTaskStatusStatistics(List<DetectionTask> tilingTasks) {
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
                          computeHealthStatistics(tilingTasks, progressionStatus, healthStatus)));
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
      List<DetectionTask> tilingTasks,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    return TaskStatusStatistic.HealthStatusStatistic.builder()
        .healthStatus(healthStatus)
        .count(
            tilingTasks.stream()
                .filter(
                    task ->
                        task.getStatus().getProgression().equals(progressionStatus)
                            && task.getStatus().getHealth().equals(healthStatus))
                .count())
        .build();
  }
}
