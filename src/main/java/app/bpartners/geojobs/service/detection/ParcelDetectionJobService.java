package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionJobStatusChanged;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ParcelDetectionJobService extends JobService<TileDetectionTask, ParcelDetectionJob> {
  protected ParcelDetectionJobService(
      JpaRepository<ParcelDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<TileDetectionTask> taskRepository,
      EventProducer eventProducer) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ParcelDetectionJob.class);
  }

  @Override
  public void onStatusChanged(ParcelDetectionJob oldJob, ParcelDetectionJob newJob) {
    eventProducer.accept(
        List.of(ParcelDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
