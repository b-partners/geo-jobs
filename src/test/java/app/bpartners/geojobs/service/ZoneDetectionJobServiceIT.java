package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.JobStatus.JobType.DETECTION;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.Tile;
import app.bpartners.geojobs.repository.model.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.ZoneDetectionTask;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobService service;
  @MockBean ZoneDetectionJobRepository repository;
  @MockBean EventProducer eventProducer;

  public ZoneDetectionJob zoneDetectionJob() {
    return ZoneDetectionJob.builder()
        .id(randomUUID().toString())
        .statusHistory(
            new ArrayList<>() {
              {
                add(
                    JobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(randomUUID().toString())
                        .jobType(DETECTION)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build());
              }
            })
        .tasks(
            List.of(
                ZoneDetectionTask.builder()
                    .id(String.valueOf(randomUUID()))
                    .jobId(String.valueOf(randomUUID()))
                    .submissionInstant(Instant.now())
                    .tile(
                        Tile.builder()
                            .id(String.valueOf(randomUUID()))
                            .bucketPath(String.valueOf(randomUUID()))
                            .build())
                    .statusHistory(
                        new ArrayList<>() {
                          {
                            add(
                                TaskStatus.builder()
                                    .id(randomUUID().toString())
                                    .progression(PENDING)
                                    .jobType(DETECTION)
                                    .health(UNKNOWN)
                                    .build());
                          }
                        })
                    .build()))
        .build();
  }

  @Test
  void process_zdj() {
    when(repository.findById(any())).thenReturn(Optional.of(zoneDetectionJob()));
    when(repository.save(any())).thenReturn(zoneDetectionJob());

    List<ZoneDetectionJob> actual = service.process(zoneDetectionJob().getId());
    var actualJob = actual.get(0);

    assertEquals(1, actual.size());
    assertNotNull(actualJob.getId());

    verify(eventProducer, only()).accept(any());
  }
}
