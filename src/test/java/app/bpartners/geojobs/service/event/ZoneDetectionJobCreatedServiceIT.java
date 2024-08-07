package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobCreatedServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobCreatedService subject;
  @Autowired ZoneDetectionJobService zoneDetectionJobService;
  @Mock ParcelDetectionTaskRepository taskRepository;
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobRepository repository;

  @Test
  void accept() {
    String jobId = randomUUID().toString();
    String taskId = randomUUID().toString();
    ZoneDetectionJob toCreate =
        ZoneDetectionJob.builder()
            .id(jobId)
            .zoneName("mock")
            .detectionType(MACHINE)
            .emailReceiver("mock@gmail.com")
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(jobId)
                        .jobType(DETECTION)
                        .progression(PENDING)
                        .health(UNKNOWN)
                        .build()))
            .build();
    ZoneDetectionJob created = repository.save(toCreate);
    ZoneDetectionJobCreated createdEventPayload =
        ZoneDetectionJobCreated.builder().zoneDetectionJob(created).build();
    when(taskRepository.findAllByJobId(jobId))
        .thenReturn(
            List.of(
                ParcelDetectionTask.builder()
                    .id(taskId)
                    .jobId(jobId)
                    .submissionInstant(now())
                    .statusHistory(
                        List.of(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .progression(PENDING)
                                .health(UNKNOWN)
                                .jobType(DETECTION)
                                .taskId(taskId)
                                .creationDatetime(now())
                                .build()))
                    .build()));

    subject.accept(createdEventPayload);
    ZoneDetectionJob actualAfterAccept = zoneDetectionJobService.findById(created.getId());

    verify(eventProducer, times(1)).accept(anyList());
    assertEquals(UNKNOWN, actualAfterAccept.getStatus().getHealth());
    assertEquals(PENDING, actualAfterAccept.getStatus().getProgression());
  }
}
