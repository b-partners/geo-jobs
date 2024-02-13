package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ZoneDetectionJobService extends JobService<DetectionTask, ZoneDetectionJob> {
  private final DetectionMapper detectionMapper;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      DetectionTaskRepository taskRepository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper) {
    super(repository, taskRepository, eventProducer, ZoneDetectionJob.class);
    this.detectionMapper = detectionMapper;
  }

  public ZoneDetectionJob fireTasks(String jobId) {
    var job = findById(jobId);
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new DetectionTaskCreated(task))));
    return job;
  }

  @Override
  protected void onStatusChanged(ZoneDetectionJob oldJob, ZoneDetectionJob newJob) {
    eventProducer.accept(
        List.of(ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  public void saveZDJFromZTJ(ZoneTilingJob job) {
    ZoneDetectionJob zoneDetectionJob = detectionMapper.fromTilingJob(job);
    repository.save(zoneDetectionJob);
  }

  public ZoneDetectionJob save(ZoneDetectionJob job) {
    return repository.save(job);
  }
}
