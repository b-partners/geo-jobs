package school.hei.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.geojobs.conf.FacadeIT;
import school.hei.geojobs.endpoint.event.EventProducer;
import school.hei.geojobs.endpoint.event.gen.ZoneDetectionJobCreated;
import school.hei.geojobs.repository.ZoneDetectionJobRepository;
import school.hei.geojobs.repository.model.DetectionJobStatus;
import school.hei.geojobs.repository.model.DetectionTaskStatus;
import school.hei.geojobs.repository.model.ZoneDetectionJob;
import school.hei.geojobs.repository.model.ZoneDetectionTask;
import school.hei.geojobs.service.ZoneDetectionJobService;

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
            .emailReceiver("mock@gmail.com")
            .tasks(
                List.of(
                    ZoneDetectionTask.builder()
                        .id(taskId)
                        .jobId(jobId)
                        .submissionInstant(now())
                        .statusHistory(
                            List.of(
                                DetectionTaskStatus.builder()
                                    .id(randomUUID().toString())
                                    .progression(PENDING)
                                    .health(UNKNOWN)
                                    .taskId(taskId)
                                    .creationDatetime(now())
                                    .build()))
                        .build()))
            .statusHistory(
                List.of(
                    DetectionJobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(jobId)
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
    assertEquals(actualAfterAccept.getStatus().getHealth(), UNKNOWN);
    assertEquals(actualAfterAccept.getStatus().getProgression(), PROCESSING);
  }
}
