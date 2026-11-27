package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.gen.annotator.endpoint.rest.model.CreateAnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.CrupdateJob;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.JobStatus;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Task;
import app.bpartners.geojobs.endpoint.rest.model.Geometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryJob;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.annotator.AnnotatorApiConf;
import app.bpartners.geojobs.service.annotator.AnnotatorUserInfoGetter;
import app.bpartners.geojobs.service.annotator.CreateAnnotationBatchExtractor;
import app.bpartners.geojobs.service.annotator.LabelConverter;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.PolygonExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
import java.math.BigDecimal;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

class AnnotationServiceTest {
  public static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  MockedConstruction<JobsApi> jobsApiMockedConstruction;
  MockedConstruction<AdminApi> adminApiMockedConstruction;
  DetectableObjectConfigurationRepository detectableObjectRepositoryMock = mock();
  LabelConverter labelConverterMock = mock();
  AnnotatorApiConf annotatorApiConfMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  AnnotationDeliveryJobService deliverJobServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  LabelExtractor labelExtractor =
      new LabelExtractor(new KeyPredicateFunction(), labelConverterMock);
  CreateAnnotationBatchExtractor batchExtractor =
      new CreateAnnotationBatchExtractor(labelExtractor, new PolygonExtractor());
  TaskExtractor taskExtractor = new TaskExtractor(batchExtractor, labelExtractor);
  AnnotatorUserInfoGetter annotatorUserInfoGetterMock = mock();
  AnnotationService subject;

  @NonNull
  private static List<DetectableObjectConfiguration> detectableObjects() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(PASSAGE_PIETON).build(),
        DetectableObjectConfiguration.builder().objectType(TOITURE_REVETEMENT).build());
  }

  @SneakyThrows
  @NonNull
  private static List<MachineDetectedTile> detectedTiles() {
    return List.of(
        MachineDetectedTile.builder().id("detectedTile1Id").detectedObjects(List.of()).build(),
        MachineDetectedTile.builder()
            .id("detectedTile2Id")
            .detectedObjects(
                List.of(
                    DetectedObject.builder()
                        .detectedObjectType(
                            DetectableObjectType.builder().detectableType(PASSAGE_PIETON).build())
                        .feature(
                            app.bpartners.geojobs.repository.model.Feature.builder()
                                .geometry(
                                    app.bpartners.geojobs.repository.model.Feature.FeatureGeometry
                                        .builder()
                                        .geometryType(Geometry.TypeEnum.MULTI_POLYGON)
                                        .actualInstanceStringValue(
                                            objectMapper()
                                                .writeValueAsString(
                                                    new MultiPolygon()
                                                        .coordinates(
                                                            List.of(
                                                                List.of(
                                                                    List.of(
                                                                        List.of(
                                                                            new BigDecimal("0.8"),
                                                                            new BigDecimal(
                                                                                "0.9"))))))))
                                        .build())
                                .build())
                        .computedConfidence(0.8)
                        .build()))
            .build());
  }

  @BeforeEach
  void setUp() {
    jobsApiMockedConstruction = mockConstruction(JobsApi.class);
    adminApiMockedConstruction = mockConstruction(AdminApi.class);
    subject =
        new AnnotationService(
            annotatorApiConfMock,
            taskExtractor,
            labelConverterMock,
            labelExtractor,
            annotatorUserInfoGetterMock,
            deliverJobServiceMock,
            objectConfigurationRepositoryMock,
            bucketComponentMock);

    when(detectableObjectRepositoryMock.findAllByDetectionJobId(ZONE_DETECTION_JOB_ID))
        .thenReturn(detectableObjects());
    when(labelConverterMock.apply(PASSAGE_PIETON))
        .thenReturn(new Label().name(PASSAGE_PIETON.name()));
    when(annotatorApiConfMock.newApiClientWithApiKey()).thenReturn(new ApiClient());
  }

  @AfterEach
  void tearDown() {
    jobsApiMockedConstruction.close();
    adminApiMockedConstruction.close();
  }

  @SneakyThrows
  @Test
  void createAnnotationJob_with_some_un_found_objects() {
    var jobsApi = jobsApiMockedConstruction.constructed().getFirst();
    when(jobsApi.saveJob(any(), any())).thenReturn(new Job().id("annotatorJobId"));
    var annotationDeliveryJobCapture = ArgumentCaptor.forClass(AnnotationDeliveryJob.class);
    var annotationDeliveryTasksCapture = ArgumentCaptor.forClass(List.class);

    subject.createAnnotationJob(humanDetectionJob());

    verify(deliverJobServiceMock, only())
        .create(annotationDeliveryJobCapture.capture(), annotationDeliveryTasksCapture.capture());

    var annotationDeliveryJobValue = annotationDeliveryJobCapture.getValue();
    var annotationDeliveryTaskValues = annotationDeliveryJobCapture.getAllValues();

    assertEquals(
        humanDetectionJob().getAnnotationJobId(), annotationDeliveryJobValue.getAnnotationJobId());
    assertFalse(annotationDeliveryTaskValues.isEmpty());
  }

  private HumanDetectionJob humanDetectionJob() {
    return HumanDetectionJob.builder()
        .id("humanDetectionJob")
        .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
        .annotationJobId("annotationJobId")
        .machineDetectedTiles(detectedTiles())
        .detectableObjectConfigurations(detectableObjects())
        .build();
  }

  private AdminApi adminApi() {
    return adminApiMockedConstruction.constructed().getFirst();
  }

  private JobsApi jobsApi() {
    return jobsApiMockedConstruction.constructed().getFirst();
  }

  private static ApiException annotatorError(String message) {
    return new ApiException(message);
  }

  @SneakyThrows
  @Test
  void get_annotation_job_by_id_returns_the_annotator_job() {
    var job = new Job().id("annotationJobId");
    when(jobsApi().getJob("annotationJobId")).thenReturn(job);

    var actual = subject.getAnnotationJobById("annotationJobId");

    assertEquals(job, actual);
  }

  @SneakyThrows
  @Test
  void get_annotation_job_by_id_reports_annotator_errors() {
    when(jobsApi().getJob("annotationJobId")).thenThrow(annotatorError("annotator down"));

    var error =
        assertThrows(
            app.bpartners.geojobs.model.exception.ApiException.class,
            () -> subject.getAnnotationJobById("annotationJobId"));

    assertEquals(
        app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION,
        error.getType());
  }

  @SneakyThrows
  @Test
  void add_annotation_task_sends_the_task_to_the_annotation_job() {
    var task = new CreateAnnotatedTask().id("taskId");

    subject.addAnnotationTask("annotationJobId", task);

    verify(adminApi()).addAnnotatedTasksToAnnotatedJob("annotationJobId", List.of(task));
  }

  @SneakyThrows
  @Test
  void add_annotation_task_reports_annotator_errors() {
    var task = new CreateAnnotatedTask().id("taskId");
    when(adminApi().addAnnotatedTasksToAnnotatedJob(any(), any()))
        .thenThrow(annotatorError("rejected"));

    assertThrows(
        app.bpartners.geojobs.model.exception.ApiException.class,
        () -> subject.addAnnotationTask("annotationJobId", task));
  }

  @SneakyThrows
  @Test
  void get_annotations_reads_the_first_page_of_500_batches_of_the_task() {
    var batches = List.of(new AnnotationBatch().id("batchId"));
    when(adminApi().getAnnotationBatchesByJobTask("annotationJobId", "taskId", 1, 500))
        .thenReturn(batches);

    var actual = subject.getAnnotations("annotationJobId", "taskId");

    assertEquals(batches, actual);
  }

  @SneakyThrows
  @Test
  void get_annotations_reports_annotator_errors() {
    when(adminApi().getAnnotationBatchesByJobTask(any(), any(), any(), any()))
        .thenThrow(annotatorError("batches unavailable"));

    assertThrows(
        app.bpartners.geojobs.model.exception.ApiException.class,
        () -> subject.getAnnotations("annotationJobId", "taskId"));
  }

  @SneakyThrows
  @Test
  void retrieve_tasks_extracts_zoom_and_tile_coordinates_from_the_task_filename() {
    when(adminApi().getJobTasks("annotationJobId", 1, 10, null, "userId"))
        .thenReturn(
            List.of(
                new Task().id("annotationTask1").filename("tile_20_544680_383095.jpg"),
                new Task().id("annotationTask2").filename("a1b2_19_100_200.jpg")));

    var actual =
        subject.retrieveTasksFromAnnotationJob(
            "humanZdjId", "retrievingJobId", "annotationJobId", 1, 10, "userId");

    assertEquals(2, actual.size());
    var first = actual.getFirst();
    assertEquals("annotationTask1", first.getAnnotationTaskId());
    assertEquals("annotationJobId", first.getAnnotationJobId());
    assertEquals("retrievingJobId", first.getJobId());
    assertEquals("humanZdjId", first.getHumanZoneDetectionJobId());
    assertEquals(20, first.getZoom());
    assertEquals(544680, first.getXTile());
    assertEquals(383095, first.getYTile());
    assertEquals(19, actual.getLast().getZoom());
    assertEquals(100, actual.getLast().getXTile());
    assertEquals(200, actual.getLast().getYTile());
  }

  @SneakyThrows
  @Test
  void retrieve_tasks_reports_annotator_errors() {
    when(adminApi().getJobTasks(any(), any(), any(), any(), any()))
        .thenThrow(annotatorError("tasks unavailable"));

    assertThrows(
        app.bpartners.geojobs.model.exception.ApiException.class,
        () ->
            subject.retrieveTasksFromAnnotationJob(
                "humanZdjId", "retrievingJobId", "annotationJobId", 1, 10, "userId"));
  }

  @SneakyThrows
  @Test
  void save_annotation_job_uses_the_default_bucket_without_configured_storage() {
    when(bucketComponentMock.getBucketName()).thenReturn("defaultBucket");
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId("detectionJobId"))
        .thenReturn(List.of(DetectableObjectConfiguration.builder().build()));
    when(annotatorUserInfoGetterMock.getTeamId()).thenReturn("teamId");

    subject.saveAnnotationJob(
        "detectionJobId",
        "annotationJobId",
        "jobName",
        List.of(new Label().name("PISCINE")),
        JobStatus.PENDING);

    var captor = ArgumentCaptor.forClass(CrupdateJob.class);
    verify(jobsApi()).saveJob(eq("annotationJobId"), captor.capture());
    var saved = captor.getValue();
    assertEquals("annotationJobId", saved.getId());
    assertEquals("jobName", saved.getName());
    assertEquals("defaultBucket", saved.getBucketName());
    assertEquals("tech@birdia.fr", saved.getOwnerEmail());
    assertEquals("teamId", saved.getTeamId());
    assertEquals(JobStatus.PENDING, saved.getStatus());
    assertEquals(1024, saved.getImagesHeight());
    assertEquals(1024, saved.getImagesWidth());
  }

  @SneakyThrows
  @Test
  void save_annotation_job_uses_the_bucket_configured_on_the_detectable_objects() {
    when(bucketComponentMock.getBucketName()).thenReturn("defaultBucket");
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId("detectionJobId"))
        .thenReturn(
            List.of(
                DetectableObjectConfiguration.builder().build(),
                DetectableObjectConfiguration.builder().bucketStorageName("customBucket").build()));

    subject.saveAnnotationJob(
        "detectionJobId", "annotationJobId", "jobName", List.of(), JobStatus.PENDING);

    var captor = ArgumentCaptor.forClass(CrupdateJob.class);
    verify(jobsApi()).saveJob(eq("annotationJobId"), captor.capture());
    assertEquals("customBucket", captor.getValue().getBucketName());
  }

  @SneakyThrows
  @Test
  void save_annotation_job_reports_annotator_errors_to_cause_a_retry() {
    when(bucketComponentMock.getBucketName()).thenReturn("defaultBucket");
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId("detectionJobId"))
        .thenReturn(List.of());
    when(jobsApi().saveJob(any(), any())).thenThrow(annotatorError("annotator down"));
    var labels = List.of(new Label().name("PISCINE"));

    var error =
        assertThrows(
            app.bpartners.geojobs.model.exception.ApiException.class,
            () ->
                subject.saveAnnotationJob(
                    "detectionJobId", "annotationJobId", "jobName", labels, JobStatus.PENDING));

    assertTrue(error.getMessage().contains("annotationJob(id=annotationJobId"));
    assertTrue(error.getMessage().contains("annotator down"));
  }
}
