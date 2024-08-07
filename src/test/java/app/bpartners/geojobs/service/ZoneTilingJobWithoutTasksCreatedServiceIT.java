package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobWithoutTasksCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ZoneTilingJobWithoutTasksCreatedService;
import app.bpartners.geojobs.service.tiling.TilingJobDuplicatedMailer;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(isolation = Isolation.SERIALIZABLE)
public class ZoneTilingJobWithoutTasksCreatedServiceIT extends FacadeIT {
  private static final String JOB_ID = "someTilingJob";
  private static final String TILING_TASK1_ID = "tilingTask1_id";
  private static final String TILING_TASK2_ID = "tilingTask2_id";
  private static final String DUPLICATED_JOB_ID = "duplicatedJobId";
  @Autowired ZoneTilingJobWithoutTasksCreatedService subject;
  @Autowired ZoneTilingJobRepository zoneTilingJobRepository;
  @Autowired TilingTaskRepository taskRepository;
  @Autowired ZoneDetectionJobRepository detectionJobRepository;
  @MockBean EventProducer eventProducer;
  @MockBean TilingJobDuplicatedMailer tilingJobDuplicatedMailerMock;

  @BeforeEach
  void setUp() {
    zoneTilingJobRepository.save(
        ZoneTilingJob.builder()
            .id(JOB_ID)
            .emailReceiver("dummy@email.com")
            .zoneName("dummyZoneName")
            .build());
    TilingTask taskWithoutParcel =
        TilingTask.builder()
            .id(TILING_TASK1_ID)
            .jobId(JOB_ID)
            .parcels(List.of())
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id("taskStatus1_id")
                        .taskId(TILING_TASK2_ID)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .build();
    TilingTask taskWithParcel =
        TilingTask.builder()
            .id(TILING_TASK2_ID)
            .jobId(JOB_ID)
            .statusHistory(
                List.of(
                    TaskStatus.builder()
                        .id("taskStatus1_id")
                        .taskId(TILING_TASK2_ID)
                        .progression(Status.ProgressionStatus.PENDING)
                        .health(Status.HealthStatus.UNKNOWN)
                        .creationDatetime(now())
                        .build()))
            .parcels(
                List.of(
                    Parcel.builder()
                        .id("parcel1_id")
                        .parcelContent(
                            ParcelContent.builder()
                                .id("parcelContent1_id")
                                .tiles(List.of(new Tile()))
                                .build())
                        .build()))
            .build();
    taskRepository.saveAll(List.of(taskWithoutParcel, taskWithParcel));
  }

  @AfterEach
  void tearDown() {
    taskRepository.deleteAllById(List.of(TILING_TASK1_ID, TILING_TASK2_ID));
    zoneTilingJobRepository.deleteById(JOB_ID);
  }

  @Test
  void accept_ok() {
    var ztj = zoneTilingJobRepository.getById(JOB_ID);
    var emptyInitialJob = zoneTilingJobRepository.findById(DUPLICATED_JOB_ID);
    var emptyInitialTasks = taskRepository.findAllByJobId(DUPLICATED_JOB_ID);
    var expectedTasks = taskRepository.findAllByJobId(JOB_ID);

    subject.accept(
        ZoneTilingJobWithoutTasksCreated.builder()
            .originalJob(ztj)
            .duplicatedJobId(DUPLICATED_JOB_ID)
            .build());

    var actualDuplicatedJob = zoneTilingJobRepository.findById(DUPLICATED_JOB_ID).orElseThrow();
    var actualDuplicatedTasks = taskRepository.findAllByJobId(DUPLICATED_JOB_ID);
    var associatedDetectionJobs =
        detectionJobRepository.findAllByZoneTilingJob_Id(DUPLICATED_JOB_ID);
    assertTrue(emptyInitialJob.isEmpty());
    assertTrue(emptyInitialTasks.isEmpty());
    assertEquals(expectedTasks.size(), actualDuplicatedTasks.size());
    // TODO: submissionInstant not identical for nanoseconds
    // assertEquals(ztj.duplicate(DUPLICATED_JOB_ID), actualDuplicatedJob);
    assertEquals(2, associatedDetectionJobs.size());
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(HUMAN)));
    assertTrue(
        associatedDetectionJobs.stream().anyMatch(job -> job.getDetectionType().equals(MACHINE)));
    verify(tilingJobDuplicatedMailerMock, times(1)).accept(any());
  }
}
