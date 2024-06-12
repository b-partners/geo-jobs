package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ParcelDetectionJobService extends JobService<TileDetectionTask, ParcelDetectionJob> {
  protected ParcelDetectionJobService(
      JpaRepository<ParcelDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<TileDetectionTask> taskRepository,
      EventProducer eventProducer,
      Class<ParcelDetectionJob> jobClazz) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, jobClazz);
  }
}
