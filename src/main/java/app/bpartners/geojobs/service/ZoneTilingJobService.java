package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.TILING;

import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingTaskCreated;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.model.ZoneTilingTask;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneTilingJobService {
  private final ZoneTilingJobRepository repository;
  private final ZoneJobService<ZoneTilingTask, ZoneTilingJob> zoneJobService;

  public ZoneTilingJob create(ZoneTilingJob job) {
    var saved = zoneJobService.create(job, repository);
    zoneJobService.getEventProducer().accept(List.of(new ZoneTilingJobCreated(saved)));
    return saved;
  }

  public List<ZoneTilingJob> findAll(PageFromOne page, BoundedPageSize pageSize) {
    return zoneJobService.findAll(page, pageSize, repository);
  }

  public ZoneTilingJob findById(String id) {
    return zoneJobService.findById(id, repository);
  }

  public List<Parcel> getAJobParcel(String jobId) {
    Optional<ZoneTilingJob> zoneTilingJob = repository.findById(jobId);
    if (zoneTilingJob.isPresent()) {
      return zoneTilingJob.get().getTasks().stream().map(ZoneTilingTask::getParcel).toList();
    }
    throw new NotFoundException("The job is not found");
  }

  public ZoneTilingJob process(ZoneTilingJob job) {
    var processed = zoneJobService.process(job, TILING, repository);
    job.getTasks()
        .forEach(
            task ->
                zoneJobService.getEventProducer().accept(List.of(new ZoneTilingTaskCreated(task))));
    return processed;
  }

  public ZoneTilingJob refreshStatus(String jobId) {
    var oldJob = zoneJobService.findById(jobId, repository);
    Status oldStatus = oldJob.getStatus();
    Status newStatus =
        Status.reduce(
            oldJob.getTasks().stream()
                .map(ZoneTilingTask::getStatus)
                .map(status -> (Status) status)
                .toList());
    if (oldStatus.equals(newStatus)) {
      return oldJob;
    }
    var jobStatus = JobStatus.from(oldJob.getId(), newStatus, TILING);
    var refreshed = zoneJobService.updateStatus(oldJob, jobStatus, repository);

    zoneJobService
        .getEventProducer()
        .accept(
            List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(refreshed).build()));
    return refreshed;
  }
}
