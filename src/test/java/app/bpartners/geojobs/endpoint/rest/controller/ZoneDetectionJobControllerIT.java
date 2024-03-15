package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ROOF;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.BoundedPageSize;
import app.bpartners.geojobs.model.PageFromOne;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

public class ZoneDetectionJobControllerIT extends FacadeIT {
  public static final String JOB1_ID = "job1";
  public static final String JOB2_ID = "job2";
  @Autowired ZoneDetectionController subject;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired JobStatusRepository jobStatusRepository;
  @Autowired DetectionTaskRepository detectionTaskRepository;
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
                .submissionInstant(now())
                .build())
        .build();
  }

  @NotNull
  private static List<app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob>
      someDetectionJobs() {
    return List.of(aZDJ(JOB1_ID), aZDJ(JOB2_ID));
  }

  private static DetectionTask someDetectionTask(
      String jobId, String taskId, String parcelId, String parcelContentId, String tileId) {
    return DetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(List.of(someParcel(parcelId, parcelContentId, tileId)))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  private static DetectionTask someDetectionTask(String jobId, String taskId) {
    return DetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(
            List.of(
                someParcel(
                    randomUUID().toString(), randomUUID().toString(), randomUUID().toString())))
        .statusHistory(
            List.of(
                TaskStatus.builder()
                    .id(randomUUID().toString())
                    .progression(PENDING)
                    .jobType(DETECTION)
                    .health(UNKNOWN)
                    .build()))
        .build();
  }

  private static Parcel someParcel(String parcelId, String parcelContentId, String tileId) {
    return Parcel.builder()
        .id(parcelId)
        .parcelContent(
            ParcelContent.builder()
                .id(parcelContentId)
                .tiles(List.of(Tile.builder().id(tileId).bucketPath("dummyBucketPath").build()))
                .build())
        .build();
  }

  private static DetectionTask detectionTask2(String jobId) {
    return someDetectionTask(jobId, "detection_task2");
  }

  private static DetectionTask detectionTask1(String jobId) {
    return someDetectionTask(jobId, "detection_task1");
  }

  @NotNull
  private static List<DetectionTask> randomDetectionTasks(String jobId) {
    return List.of(detectionTask1(jobId), detectionTask2(jobId));
  }

  @AfterEach
  void tearDown() {
    jobRepository.deleteAll(someDetectionJobs());
    detectionTaskRepository.deleteAll(randomDetectionTasks(JOB1_ID));
    jobRepository.deleteById("random_job_status2");
  }

  @Test
  void read_zdj_geo_jobs_url() {
    jobRepository.saveAll(List.of(aZDJ(JOB1_ID), aZDJ(JOB2_ID)));
    jobStatusRepository.save(
        JobStatus.builder()
            .id("random_job_status2")
            .jobId(JOB2_ID)
            .jobType(DETECTION)
            .progression(FINISHED)
            .health(SUCCEEDED)
            .build());

    GeoJsonsUrl actual1 = subject.getZDJGeojsonsUrl(JOB1_ID);
    GeoJsonsUrl actual2 = subject.getZDJGeojsonsUrl(JOB2_ID);

    assertEquals(
        new GeoJsonsUrl()
            .url(null)
            .status(
                new Status()
                    .progression(Status.ProgressionEnum.PENDING)
                    .health(Status.HealthEnum.UNKNOWN)
                    .creationDatetime(null)),
        actual1.status(actual1.getStatus().creationDatetime(null)));
    assertEquals(
        new GeoJsonsUrl()
            .url("NotImplemented: finished human detection job without url")
            .status(
                new Status()
                    .progression(Status.ProgressionEnum.FINISHED)
                    .health(Status.HealthEnum.SUCCEEDED)
                    .creationDatetime(null)),
        actual2.status(actual2.getStatus().creationDatetime(null)));
  }

  @Test
  void read_detection_jobs() {
    var savedJobs = jobRepository.saveAll(someDetectionJobs());
    var expected =
        savedJobs.stream().map(job -> detectionJobMapper.toRest(job, List.of())).toList();
    List<ZoneDetectionJob> actual =
        subject.getDetectionJobs(
            new PageFromOne(PageFromOne.MIN_PAGE), new BoundedPageSize(BoundedPageSize.MAX_SIZE));

    assertNotNull(actual);
    assertEquals(2, actual.size());
    assertEquals(expected, actual);
  }

  @Test
  @Transactional
  void process_zdj() {
    var job1 = jobRepository.saveAll(someDetectionJobs()).get(0);
    var configuredTasks = detectionTaskRepository.saveAll(randomDetectionTasks(job1.getId()));
    var detectableObjectConfig =
        List.of(new DetectableObjectConfiguration().type(ROOF).confidence(new BigDecimal("0.75")));
    var expected =
        detectionJobMapper.toRest(job1, List.of()).objectsToDetect(detectableObjectConfig);

    ZoneDetectionJob actual = subject.processZDJ(job1.getId(), detectableObjectConfig);

    assertEquals(expected, actual);
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(configuredTasks.size())).accept(eventsCaptor.capture());
    var events = eventsCaptor.getAllValues();
    var capturedEvent1 = events.get(0).get(0);
    var capturedEvent2 = events.get(1).get(0);
    assertEquals(new DetectionTaskCreated(configuredTasks.get(0)), capturedEvent1);
    assertEquals(new DetectionTaskCreated(configuredTasks.get(1)), capturedEvent2);
  }

  @Test
  @Transactional
  void read_zdj_parcels() {
    jobRepository.saveAll(someDetectionJobs());
    var savedTask =
        detectionTaskRepository.save(
            someDetectionTask(JOB1_ID, "task1", "parcel1", "parcelContent1", "tile1"));
    var status =
        new Status().progression(Status.ProgressionEnum.PENDING).health(Status.HealthEnum.UNKNOWN);
    var expected =
        new DetectedParcel()
            .id(null) // TODO: actually randomly computed
            .detectionJobIb(JOB1_ID)
            .parcelId("parcel1")
            // .detectedTiles(List.of(new
            // DetectedTile().tileId("tile1").bucketPath("dummyBucketPath"))) TODO: link to Parcel
            // Detection Task
            .detectedTiles(List.of())
            .status(status);

    List<DetectedParcel> actual = subject.getZDJParcels(JOB1_ID);

    assertNotNull(actual);
    assertEquals(
        expected
            .status(
                status.creationDatetime(actual.get(0).getStatus().getCreationDatetime())) // ignore
            .creationDatetime(actual.get(0).getCreationDatetime()), // ignore
        actual.get(0).id(expected.getId()) // TODO: actually randomly computed
        );

    // TODO: reset database correctly
    detectionTaskRepository.delete(savedTask);
  }
}
