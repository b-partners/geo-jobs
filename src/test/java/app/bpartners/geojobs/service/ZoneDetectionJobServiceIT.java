package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.geo.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geo.tiling.Tile;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobService service;
  @MockBean ZoneDetectionJobRepository repository;
  @MockBean DetectionTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;

  public ZoneDetectionJob aZDJ(String jobId) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(DETECTION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return ZoneDetectionJob.builder().id(jobId).statusHistory(statusHistory).build();
  }

  @Test
  void process_zdj() {
    var jobId = randomUUID().toString();
    var job = aZDJ(jobId);
    when(repository.findById(any())).thenReturn(Optional.of(job));
    when(repository.save(any())).thenReturn(job);
    var taskStatusHistory = new ArrayList<TaskStatus>();
    taskStatusHistory.add(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .progression(PENDING)
            .jobType(DETECTION)
            .health(UNKNOWN)
            .build());
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                DetectionTask.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .submissionInstant(now())
                    .tile(
                        Tile.builder()
                            .id(randomUUID().toString())
                            .bucketPath(randomUUID().toString())
                            .build())
                    .statusHistory(taskStatusHistory)
                    .build()));

    ZoneDetectionJob actual = service.fireTasks(jobId);

    assertNotNull(actual.getId());
    verify(eventProducer, only()).accept(any());
  }
}
