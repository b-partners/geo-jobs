package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.geo.JobType.DETECTION;
import static app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobCreated;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ZoneDetectionJobCreatedServiceIT extends FacadeIT {
  @Autowired ZoneDetectionJobCreatedService subject;
  @Autowired ZoneDetectionJobService zoneDetectionJobService;
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
            .type(MACHINE)
            .emailReceiver("mock@gmail.com")
            .tasks(
                List.of(
                    DetectionTask.builder()
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
                        .build()))
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

    subject.accept(createdEventPayload);
    ZoneDetectionJob actualAfterAccept = zoneDetectionJobService.findById(created.getId());

    int numberOfFeaturesInJob = 1;
    verify(eventProducer, times(numberOfFeaturesInJob)).accept(anyList());
    assertEquals(UNKNOWN, actualAfterAccept.getStatus().getHealth());
    assertEquals(PENDING, actualAfterAccept.getStatus().getProgression());
  }
}
