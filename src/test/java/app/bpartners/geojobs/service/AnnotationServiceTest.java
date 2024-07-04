package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiClient;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.service.annotator.*;
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
  MockedConstruction<JobsApi> jobsApiMockedConstruction;
  DetectableObjectConfigurationRepository detectableObjectRepositoryMock = mock();
  LabelConverter labelConverterMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotatorApiConf annotatorApiConfMock = mock();
  LabelExtractor labelExtractorMock =
      new LabelExtractor(new KeyPredicateFunction(), labelConverterMock);
  CreateAnnotationBatchExtractor batchExtractorMock =
      new CreateAnnotationBatchExtractor(labelExtractorMock, new PolygonExtractor());
  TaskExtractor taskExtractorMock = new TaskExtractor(batchExtractorMock, labelExtractorMock);
  public static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";

  @BeforeEach
  void setUp() {
    jobsApiMockedConstruction = mockConstruction(JobsApi.class);

    when(detectableObjectRepositoryMock.findAllByDetectionJobId(ZONE_DETECTION_JOB_ID))
        .thenReturn(detectableObjects());
    when(labelConverterMock.apply(PATHWAY)).thenReturn(new Label().name("PATHWAY"));
    when(annotatorApiConfMock.newApiClientWithApiKey()).thenReturn(new ApiClient());
  }

  @NonNull
  private static List<DetectableObjectConfiguration> detectableObjects() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(PATHWAY).build(),
        DetectableObjectConfiguration.builder().objectType(ROOF).build());
  }

  @AfterEach
  void tearDown() {
    jobsApiMockedConstruction.close();
  }

  @SneakyThrows
  @Test
  void createAnnotationJob_with_some_un_found_objects() {
    AnnotationService subject =
        new AnnotationService(
            annotatorApiConfMock,
            taskExtractorMock,
            labelConverterMock,
            labelExtractorMock,
            mock(),
            mock(),
            eventProducerMock);
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
                                                            new BigDecimal(0.8),
                                                            new BigDecimal(0.9))))))))
                        .computedConfidence(0.8)
                        .build()))
            .build());
  }
}
