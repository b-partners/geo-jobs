package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.geo.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobCreated;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.tiling.ZoneTilingJobService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneTilingJobCreatedServiceIT extends FacadeIT {
  @Autowired ZoneTilingJobCreatedService subject;
  @Autowired ZoneTilingJobService zoneTilingJobService;
  @MockBean EventProducer eventProducer;
  @MockBean TilingTaskRepository taskRepository;
  @Autowired ZoneTilingJobRepository repository;

  @Test
  void accept() {
    String jobId = randomUUID().toString();
    String taskId = randomUUID().toString();
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(TILING)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    ZoneTilingJob toCreate =
        ZoneTilingJob.builder()
            .id(jobId)
            .zoneName("mock")
            .emailReceiver("mock@gmail.com")
            .statusHistory(statusHistory)
            .build();
    ZoneTilingJob created = repository.save(toCreate);
    ZoneTilingJobCreated createdEventPayload =
        ZoneTilingJobCreated.builder().zoneTilingJob(created).build();
    when(taskRepository.findAllByJobId(created.getId()))
        .thenReturn(
            List.of(
                TilingTask.builder()
                    .id(taskId)
                    .jobId(jobId)
                    .submissionInstant(now())
                    .parcel(Parcel.builder().id(randomUUID().toString()).build())
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(PENDING)
                                .health(UNKNOWN)
                                .taskId(taskId)
                                .creationDatetime(now())
                                .build()))
                    .build()));

    subject.accept(createdEventPayload);
    ZoneTilingJob actualAfterAccept = zoneTilingJobService.findById(created.getId());

    int numberOfFeaturesInJob = 1;
    verify(eventProducer, times(numberOfFeaturesInJob)).accept(anyList());
    assertEquals(UNKNOWN, actualAfterAccept.getStatus().getHealth());
    assertEquals(PENDING, actualAfterAccept.getStatus().getProgression());
  }
}
