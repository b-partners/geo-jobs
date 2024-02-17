package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class DetectionTaskStatusService extends TaskStatusService<DetectionTask, ZoneDetectionJob> {

  public DetectionTaskStatusService(
      JpaRepository<DetectionTask, String> repository,
      JobService<DetectionTask, ZoneDetectionJob> zoneJobService) {
    super(repository, zoneJobService);
  }

  @Bean
  public RetryableTaskStatusService<DetectionTask, ZoneDetectionJob>
      getRetryableDetectionTaskStatusService(DetectionTaskStatusService taskStatusService) {
    return new RetryableTaskStatusService<>(taskStatusService);
  }
}
