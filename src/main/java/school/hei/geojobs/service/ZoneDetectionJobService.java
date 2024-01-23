package school.hei.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import java.util.List;
import org.springframework.stereotype.Service;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import school.hei.geojobs.endpoint.event.gen.ZoneDetectionTaskCreated;
import school.hei.geojobs.repository.ZoneDetectionJobRepository;
import school.hei.geojobs.repository.model.DetectionJobStatus;
import school.hei.geojobs.repository.model.Status;
import school.hei.geojobs.repository.model.ZoneDetectionJob;
import school.hei.geojobs.repository.model.ZoneDetectionTask;

@Service
public class ZoneDetectionJobService
    extends AbstractZoneJobService<
        DetectionJobStatus, ZoneDetectionTask, ZoneDetectionJob, ZoneDetectionJobRepository> {

  public ZoneDetectionJobService(
      EventProducer eventProducer, ZoneDetectionJobRepository repository) {
    super(eventProducer, repository);
  }

  public List<ZoneDetectionJob> process(String jobId) {
    var optionalZDJ = findById(jobId);
    var jobStatus =
        DetectionJobStatus.builder()
            .id(randomUUID().toString())
            .jobId(optionalZDJ.getId())
            .progression(PROCESSING)
            .health(UNKNOWN)
            .creationDatetime(now())
            .build();
    var toProcess = updateStatus(optionalZDJ, jobStatus);
    toProcess
        .getTasks()
        .forEach(task -> getEventProducer().accept(List.of(new ZoneDetectionTaskCreated(task))));
    return List.of(optionalZDJ);
  }

  public ZoneDetectionJob refreshStatus(String jobId) {
    var oldJob = findById(jobId);
    Status oldStatus = oldJob.getStatus();
    Status newStatus =
        Status.reduce(
            oldJob.getTasks().stream()
                .map(ZoneDetectionTask::getStatus)
                .map(status -> (Status) status)
                .toList());
    if (oldStatus.equals(newStatus)) {
      return oldJob;
    }

    var jobStatus = DetectionJobStatus.from(oldJob.getId(), newStatus);
    var refreshed = updateStatus(oldJob, jobStatus);

    getEventProducer()
        .accept(
            List.of(
                ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(refreshed).build()));
    return refreshed;
  }
}
