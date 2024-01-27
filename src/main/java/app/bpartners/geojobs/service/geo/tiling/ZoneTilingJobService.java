package app.bpartners.geojobs.service.geo.tiling;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.TilingTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobService;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ZoneTilingJobService extends JobService<TilingTask, ZoneTilingJob> {

  public ZoneTilingJobService(
      JpaRepository<ZoneTilingJob, String> repository, EventProducer eventProducer) {
    super(repository, eventProducer);
  }

  @Override
  public ZoneTilingJob create(ZoneTilingJob job) {
    var saved = super.create(job);
    eventProducer.accept(List.of(new ZoneTilingJobCreated(saved)));
    return saved;
  }

  public void fireTasks(ZoneTilingJob job) {
    job.getTasks().forEach(task -> eventProducer.accept(List.of(new TilingTaskCreated(task))));
  }

  @Override
  protected void onStatusChanged(ZoneTilingJob oldJob, ZoneTilingJob newJob) {
    eventProducer.accept(
        List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }
}
