package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Task;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.annotator.AnnotatorApiConf;
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

public class AnnotationServiceTest {
  public static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  public static final String ANNOTATION_JOB_ID = "AnnotationJobId";
  MockedConstruction<JobsApi> jobsApiMockedConstruction;
  MockedConstruction<AdminApi> adminApiMockedConstruction;
  DetectableObjectConfigurationRepository detectableObjectRepositoryMock = mock();
  LabelConverter labelConverterMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotatorApiConf annotatorApiConfMock = mock();
  LabelExtractor labelExtractorMock =
      new LabelExtractor(new KeyPredicateFunction(), labelConverterMock);
  CreateAnnotationBatchExtractor batchExtractorMock =
      new CreateAnnotationBatchExtractor(labelExtractorMock, new PolygonExtractor());
  TaskExtractor taskExtractorMock = new TaskExtractor(batchExtractorMock, labelExtractorMock);
  AnnotationService subject;

  @NonNull
  private static List<DetectableObjectConfiguration> detectableObjects() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(PATHWAY).build(),
        DetectableObjectConfiguration.builder().objectType(ROOF).build());
  }

  @NonNull
  private static List<DetectedTile> detectedTiles() {
    return List.of(
        DetectedTile.builder().id("detectedTile1Id").detectedObjects(List.of()).build(),
        DetectedTile.builder()
            .id("detectedTile2Id")
            .detectedObjects(
                List.of(
                    DetectedObject.builder()
                        .detectedObjectTypes(
                            List.of(DetectableObjectType.builder().detectableType(PATHWAY).build()))
                        .feature(
                            new Feature()
                                .geometry(
                                    new MultiPolygon()
                                        .coordinates(
                                            List.of(
                                                List.of(
                                                    List.of(
                                                        List.of(
                                                            new BigDecimal("0.8"),
                                                            new BigDecimal("0.9"))))))))
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
            taskExtractorMock,
            labelConverterMock,
            labelExtractorMock,
            mock(),
            detectableObjectRepositoryMock,
            mock(),
            mock(),
            eventProducerMock);

    when(detectableObjectRepositoryMock.findAllByDetectionJobId(ZONE_DETECTION_JOB_ID))
        .thenReturn(detectableObjects());
    when(labelConverterMock.apply(PATHWAY)).thenReturn(new Label().name("PATHWAY"));
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

    subject.createAnnotationJob(
        HumanDetectionJob.builder()
            .id("humanDetectionJob")
            .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
            .annotationJobId("annotationJobId")
            .detectedTiles(detectedTiles())
            .detectableObjectConfigurations(detectableObjects())
            .build());

    var eventCapture = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(2)).accept(eventCapture.capture()); // detectedTiles().size()
    CreateAnnotatedTaskSubmitted annotatedTaskExtracted1 =
        (CreateAnnotatedTaskSubmitted) eventCapture.getValue().get(0);
    CreateAnnotatedTaskSubmitted annotatedTaskExtracted2 =
        (CreateAnnotatedTaskSubmitted) eventCapture.getValue().get(0);
    assertEquals(
        PATHWAY.name(),
        annotatedTaskExtracted1
            .getCreateAnnotatedTask()
            .getAnnotationBatch()
            .getAnnotations()
            .getFirst()
            .getLabel()
            .getName());
    assertEquals(
        PATHWAY.name(),
        annotatedTaskExtracted2
            .getCreateAnnotatedTask()
            .getAnnotationBatch()
            .getAnnotations()
            .getFirst()
            .getLabel()
            .getName());
  }

  Task annotationTask1() {
    return new Task().id(randomUUID().toString()).filename("Cannes5/20/545266/251451.jpeg");
  }

  Task annotationTask2() {
    return new Task().id(randomUUID().toString()).filename("Cannes/20/545266/251452.jpeg");
  }

  @Test
  void fire_task_ok() throws ApiException {
    var imageSize = 1024;
    var tasks = List.of(annotationTask1(), annotationTask2());
    var eventsCapture = ArgumentCaptor.forClass(List.class);
    var adminApi = adminApiMockedConstruction.constructed().getFirst();
    when(adminApi.getJobTasks(any(), any(), any(), any(), any())).thenReturn(tasks);

    subject.fireTasks(ZONE_DETECTION_JOB_ID, ANNOTATION_JOB_ID, imageSize);
    verify(eventProducerMock, times(tasks.size())).accept(eventsCapture.capture());
    var eventsValues = eventsCapture.getAllValues();

    assertInstanceOf(AnnotationBatchRetrievingSubmitted.class, eventsValues.getFirst().getFirst());
  }
}
