package app.bpartners.geojobs.service.detection;

import static java.time.temporal.ChronoUnit.MINUTES;

import app.bpartners.geojobs.job.repository.TaskStatusRepository;
import app.bpartners.geojobs.job.service.RetryableTaskToTaskStatusService;
import app.bpartners.geojobs.job.service.TaskToTaskStatusService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class TileDetectionTaskStatusService
    extends TaskToTaskStatusService<TileDetectionTask, DetectionTask, ZoneDetectionJob> {
  public TileDetectionTaskStatusService(
      DetectionTaskStatusService detectionTaskStatusService,
      TaskStatusRepository taskStatusRepository,
      TileDetectionTaskService taskToTaskService) {
    super(detectionTaskStatusService, taskStatusRepository, taskToTaskService);
  }

  @Bean
  public RetryableTaskToTaskStatusService<TileDetectionTask, DetectionTask, ZoneDetectionJob>
      getRetryableTileDetectionTaskStatusService(
          TileDetectionTaskStatusService tileDetectionTaskStatusService,
          @Value("${jobs.status.update.max.sleep.minutes:1}") int maxSleepDurationInMinutes,
          @Value("${jobs.status.update.retry.max.attempt:5}") int maxRetry) {
    return new RetryableTaskToTaskStatusService<>(
        tileDetectionTaskStatusService, Duration.of(maxSleepDurationInMinutes, MINUTES), maxRetry);
  }
}
