package app.bpartners.geojobs.service.geo.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.geo.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ZoneDetectionJobService extends JobService<DetectionTask, ZoneDetectionJob> {
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final DetectionMapper detectionMapper;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper) {
    super(repository, eventProducer);
    this.zoneDetectionJobRepository = (ZoneDetectionJobRepository) repository;
    this.detectionMapper = detectionMapper;
  }

  public ZoneDetectionJob fireTasks(String jobId) {
    var job = findById(jobId);
    job.getTasks().forEach(task -> eventProducer.accept(List.of(new DetectionTaskCreated(task))));
    return job;
  }

  @Override
  protected void onStatusChanged(ZoneDetectionJob oldJob, ZoneDetectionJob newJob) {
    eventProducer.accept(
        List.of(ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  public void saveZoneDetectionJobFromZTJ(ZoneTilingJob job) {
    ZoneDetectionJob zoneDetectionJob = detectionMapper.fromTilingJob(job);
    zoneDetectionJobRepository.save(zoneDetectionJob);
  }
}
