package app.bpartners.geojobs.endpoint.rest.controller;

import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.ROOF;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AnnotationTaskRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.ZDJParcelsStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneDetectionJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.model.page.BoundedPageSize;
import app.bpartners.geojobs.model.page.PageFromOne;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.ParcelRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
  public static final String JOB3_ID = "job3";
  public static final String JOB4_ID = "job4";
  public static final String ANNOTATION_JOB_ID = "annotationJobId";
  @Autowired ZoneDetectionController subject;
  @Autowired ZoneDetectionJobRepository jobRepository;
  @Autowired JobStatusRepository jobStatusRepository;
  @Autowired ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  @Autowired ZoneDetectionJobMapper detectionJobMapper;
  @Autowired ParcelRepository parcelRepository;
  @MockBean EventProducer eventProducer;
  @MockBean AnnotationService annotationServiceMock;
  @MockBean HumanDetectionJobRepository humanDetectionJobRepositoryMock;

  static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob aZDJ(
      String jobId, String tilingJobId) {
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
                .id(tilingJobId)
                .emailReceiver("dummy@email.com")
                .zoneName("dummyZoneName")
                .submissionInstant(now())
                .build())
        .build();
  }

  @NotNull
  private static List<app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob>
      someDetectionJobs() {
    String tilingJobId1 = randomUUID().toString();
    String tilingJobId2 = randomUUID().toString();
    return List.of(
        aZDJ(JOB1_ID, tilingJobId1),
        aZDJ(JOB2_ID, tilingJobId2),
        aZDJ(randomUUID().toString(), tilingJobId1).toBuilder().detectionType(HUMAN).build(),
        aZDJ(randomUUID().toString(), tilingJobId2).toBuilder().detectionType(HUMAN).build());
  }

  public static ParcelDetectionTask someDetectionTask(
      String jobId, String taskId, String parcelId, String parcelContentId, String tileId) {
    return ParcelDetectionTask.builder()
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

  public static ParcelDetectionTask someDetectionTask(String jobId, String taskId) {
    return someDetectionTask(jobId, taskId, randomUUID().toString());
  }

  public static ParcelDetectionTask someDetectionTask(
      String jobId, String taskId, String parcelId) {
    return ParcelDetectionTask.builder()
        .id(taskId)
        .jobId(jobId)
        .parcels(List.of(someParcel(parcelId, randomUUID().toString(), randomUUID().toString())))
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

  private static ParcelDetectionTask detectionTask2(String jobId) {
    return someDetectionTask(jobId, "detection_task2");
  }

  private static ParcelDetectionTask detectionTask1(String jobId) {
    return someDetectionTask(jobId, "detection_task1");
  }

  @NotNull
  private static List<ParcelDetectionTask> randomDetectionTasks(String jobId) {
    return List.of(detectionTask1(jobId), detectionTask2(jobId));
  }

  @AfterEach
  void tearDown() {
    jobRepository.deleteAll(someDetectionJobs());
    jobRepository.deleteById("random_job_status2");
  }

  @Test
  void check_annotation_job_status_completed() {
    var tilingJobId = randomUUID().toString();
    var eventCapture = ArgumentCaptor.forClass(List.class);

    jobRepository.saveAll(
        List.of(
            aZDJ(JOB3_ID, tilingJobId).toBuilder()
                .submissionInstant(now())
                .detectionType(MACHINE)
                .build(),
            aZDJ(JOB1_ID, tilingJobId).toBuilder()
                .submissionInstant(now())
                .detectionType(HUMAN)
                .build()));
    when(humanDetectionJobRepositoryMock.findByZoneDetectionJobId(JOB1_ID))
        .thenReturn(
            List.of(HumanDetectionJob.builder().annotationJobId(ANNOTATION_JOB_ID).build()));
    when(annotationServiceMock.getAnnotationJobById(ANNOTATION_JOB_ID))
        .thenReturn(
            new Job().status(app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.COMPLETED));
    ZoneDetectionJob actual = subject.checkHumanDetectionJobStatus(JOB3_ID);
    verify(eventProducer, times(1)).accept(eventCapture.capture());
    var eventsSent = eventCapture.getAllValues();
    var taskRetrievingSubmitted = eventsSent.getFirst().getFirst();

    assertEquals(taskRetrievingSubmitted.getClass(), AnnotationTaskRetrievingSubmitted.class);
    assertEquals(new AnnotationTaskRetrievingSubmitted(JOB3_ID, JOB1_ID), taskRetrievingSubmitted);
    assertEquals(JOB1_ID, actual.getId());
    assertEquals(
        new Status()
            .progression(Status.ProgressionEnum.FINISHED)
            .health(Status.HealthEnum.SUCCEEDED)
            .creationDatetime(actual.getStatus().getCreationDatetime()),
        actual.getStatus());

    jobRepository.deleteAllById(List.of(JOB1_ID, JOB3_ID));
  }

  @Test
  void check_annotation_job_status_failed() {
    var tilingJobId = randomUUID().toString();
    jobRepository.saveAll(
        List.of(
            aZDJ(JOB3_ID, tilingJobId).toBuilder().detectionType(MACHINE).build(),
            aZDJ(JOB1_ID, tilingJobId).toBuilder().detectionType(HUMAN).build()));
    when(humanDetectionJobRepositoryMock.findByZoneDetectionJobId(JOB1_ID))
        .thenReturn(
            List.of(HumanDetectionJob.builder().annotationJobId(ANNOTATION_JOB_ID).build()));
    when(annotationServiceMock.getAnnotationJobById(ANNOTATION_JOB_ID))
        .thenReturn(
            new Job().status(app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.FAILED));
    ZoneDetectionJob actual = subject.checkHumanDetectionJobStatus(JOB3_ID);

    assertEquals(JOB1_ID, actual.getId());
    assertEquals(
        new Status()
            .progression(Status.ProgressionEnum.FINISHED)
            .health(Status.HealthEnum.FAILED)
            .creationDatetime(actual.getStatus().getCreationDatetime()),
        actual.getStatus());

    jobRepository.deleteAllById(List.of(JOB1_ID, JOB3_ID));
  }

  @Test
  void check_annotation_job_status_processing() {
    var tilingJobId = randomUUID().toString();
    jobRepository.saveAll(
        List.of(
            aZDJ(JOB3_ID, tilingJobId).toBuilder().detectionType(MACHINE).build(),
            aZDJ(JOB1_ID, tilingJobId).toBuilder().detectionType(HUMAN).build()));
    when(humanDetectionJobRepositoryMock.findByZoneDetectionJobId(JOB1_ID))
        .thenReturn(
            List.of(HumanDetectionJob.builder().annotationJobId(ANNOTATION_JOB_ID).build()));
    when(annotationServiceMock.getAnnotationJobById(ANNOTATION_JOB_ID))
        .thenReturn(
            new Job().status(app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.STARTED));
    ZoneDetectionJob actual = subject.checkHumanDetectionJobStatus(JOB3_ID);

    assertEquals(JOB1_ID, actual.getId());
    assertEquals(
        new Status()
            .progression(Status.ProgressionEnum.PROCESSING)
            .health(Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getStatus().getCreationDatetime()),
        actual.getStatus());

    jobRepository.deleteAllById(List.of(JOB1_ID, JOB3_ID));
  }

  @Test
  void check_annotation_job_status_pending() {
    var tilingJobId = randomUUID().toString();
    jobRepository.saveAll(
        List.of(
            aZDJ(JOB3_ID, tilingJobId).toBuilder().detectionType(MACHINE).build(),
            aZDJ(JOB1_ID, tilingJobId).toBuilder().detectionType(HUMAN).build()));
    when(humanDetectionJobRepositoryMock.findByZoneDetectionJobId(JOB1_ID))
        .thenReturn(
            List.of(HumanDetectionJob.builder().annotationJobId(ANNOTATION_JOB_ID).build()));
    when(annotationServiceMock.getAnnotationJobById(ANNOTATION_JOB_ID))
        .thenReturn(
            new Job().status(app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.TO_REVIEW));
    ZoneDetectionJob actual = subject.checkHumanDetectionJobStatus(JOB3_ID);

    assertEquals(JOB1_ID, actual.getId());
    assertEquals(
        new Status()
            .progression(Status.ProgressionEnum.PENDING)
            .health(Status.HealthEnum.UNKNOWN)
            .creationDatetime(actual.getStatus().getCreationDatetime()),
        actual.getStatus());

    jobRepository.deleteAllById(List.of(JOB1_ID, JOB3_ID));
  }

  @Test
  void read_zdj_geo_jobs_url() {
    var tilingJobId1 = randomUUID().toString();
    var tilingJobId2 = randomUUID().toString();
    var eventCapture = ArgumentCaptor.forClass(List.class);

    jobRepository.saveAll(
        List.of(
            aZDJ(JOB3_ID, tilingJobId1).toBuilder().detectionType(MACHINE).build(),
            aZDJ(JOB1_ID, tilingJobId1).toBuilder().detectionType(HUMAN).build(),
            aZDJ(JOB4_ID, tilingJobId2).toBuilder().detectionType(MACHINE).build(),
            aZDJ(JOB2_ID, tilingJobId2).toBuilder().detectionType(HUMAN).build()));
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

    assertNotNull(actual1.getUrl());
    assertTrue(
        actual1
            .getUrl()
            .contains(
                "Unable to generate geoJsons Url to unfinished succeeded job. Actual status is"));
    assertNull(actual1.getStatus());
    verify(eventProducer, times(1)).accept(eventCapture.capture());
    assertEquals(
        new GeoJsonsUrl()
            .url(null)
            .status(
                new Status()
                    .progression(Status.ProgressionEnum.PENDING)
                    .health(Status.HealthEnum.UNKNOWN)
                    .creationDatetime(null)),
        actual2.status(actual2.getStatus().creationDatetime(null)));

    jobRepository.deleteAllById(List.of(JOB1_ID, JOB2_ID, JOB3_ID, JOB4_ID));
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
    assertEquals(4, actual.size());
    assertEquals(expected, actual);
  }

  @Test
  @Transactional
  void process_zdj() {
    var job1 = jobRepository.saveAll(someDetectionJobs()).get(0);
    List<ParcelDetectionTask> parcelDetectionTasks = randomDetectionTasks(job1.getId());
    List<Parcel> parcels =
        parcelDetectionTasks.stream()
            .flatMap(task -> task.getParcels().stream())
            .collect(Collectors.toList());
    parcelRepository.saveAll(parcels);
    var configuredTasks = parcelDetectionTaskRepository.saveAll(parcelDetectionTasks);
    var detectableObjectConfig =
        List.of(new DetectableObjectConfiguration().type(ROOF).confidence(new BigDecimal("0.75")));
    var expected =
        detectionJobMapper.toRest(job1, List.of()).objectsToDetect(detectableObjectConfig);

    ZoneDetectionJob actual = subject.processZDJ(job1.getId(), detectableObjectConfig);

    assertEquals(expected, actual);
    var eventsCaptor = ArgumentCaptor.forClass(List.class);
    var parcelStatusEventNb = 1;
    var zdjStatusEventNb = 1;
    var taskStatusComputingEvent = 1;
    verify(
            eventProducer,
            times(
                configuredTasks.size()
                    + parcelStatusEventNb
                    + zdjStatusEventNb
                    + taskStatusComputingEvent))
        .accept(eventsCaptor.capture());
    var events = eventsCaptor.getAllValues();
    var capturedEvent1 = events.get(0).get(0);
    var capturedEvent2 = events.get(1).get(0);
    var capturedEvent3 = events.get(2).get(0);
    var capturedEvent4 = events.get(3).get(0);
    assertEquals(new ParcelDetectionTaskCreated(configuredTasks.get(0)), capturedEvent1);
    assertEquals(new ParcelDetectionTaskCreated(configuredTasks.get(1)), capturedEvent2);
    assertEquals(new ZDJParcelsStatusRecomputingSubmitted(job1.getId()), capturedEvent3);
    assertEquals(new ZDJStatusRecomputingSubmitted(job1.getId()), capturedEvent4);
  }

  @Test
  @Transactional
  void read_zdj_parcels() {
    jobRepository.saveAll(someDetectionJobs());
    ParcelDetectionTask parcelDetectionTask =
        someDetectionTask(JOB1_ID, "task1", "parcel1", "parcelContent1", "tile1");
    parcelRepository.saveAll(parcelDetectionTask.getParcels());
    var savedTask = parcelDetectionTaskRepository.save(parcelDetectionTask);
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
    parcelDetectionTaskRepository.delete(savedTask);
  }
}
