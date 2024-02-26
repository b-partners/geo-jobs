package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.ZoneDetectionJob;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ZoneDetectionJobControllerIT extends FacadeIT {
  @Autowired ZoneDetectionController subject;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired ZoneDetectionJobMapper detectionJobMapper;
  @MockBean EventProducer eventProducer;

  static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob aZDJ(String jobId) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(DETECTION)
            .progression(PENDING)
            .health(UNKNOWN)
            .build());
    return app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.builder()
        .id(jobId)
        .statusHistory(statusHistory)
        .zoneTilingJob(
            ZoneTilingJob.builder()
                .id(randomUUID().toString())
                .emailReceiver("dummy@email.com")
                .zoneName("dummyZoneName")
                .build())
        .build();
  }

  @NotNull
  private static List<app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob>
      randomDetectionJobs() {
    app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob job1 =
        aZDJ(randomUUID().toString());
    app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob job2 =
        aZDJ(randomUUID().toString());
    return List.of(job1, job2);
  }

  @AfterEach
  void tearDown() {
    jobRepository.deleteAll(randomDetectionJobs());
  }

  @Test
  void read_detection_jobs() {
    var savedJobs = jobRepository.saveAll(randomDetectionJobs());
    var expected =
        savedJobs.stream().map(job -> detectionJobMapper.toRest(job, List.of())).toList();
    List<ZoneDetectionJob> actual =
        subject.getDetectionJobs(
            new PageFromOne(PageFromOne.MIN_PAGE), new BoundedPageSize(BoundedPageSize.MAX_SIZE));

    assertNotNull(actual);
    assertEquals(2, actual.size());
    assertEquals(expected, actual);
  }
}
