package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static app.bpartners.geojobs.service.annotator.ExtractorIT.PARCEL_MOCK_ID;
import static app.bpartners.geojobs.service.event.TilingTaskCreatedServiceIT.MOCK_FEATURE_AS_STRING;
import static app.bpartners.geojobs.service.event.ZoneDetectionJobSucceededService.DEFAULT_MIN_CONFIDENCE;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ZoneDetectionAnnotationProcessorTest extends FacadeIT {
  public static final String LAYER_20_10_1_PNG = "layer/20/10/1.png";
  @Autowired ZoneDetectionJobAnnotationProcessor subject;
  @Autowired private ObjectMapper om;
  @MockBean DetectedTileRepository detectedTileRepositoryMock;
  @MockBean DetectableObjectConfigurationRepository objectConfigurationRepositoryMock;
  @MockBean AnnotationService annotationServiceMock;
  @MockBean BucketComponent selfBucketComponentMock;
  @MockBean TaskExtractor taskExtractorMock;
  @MockBean LabelExtractor labelExtractorMock;
  @MockBean HumanDetectionJobRepository humanDetectionJobRepositoryMock;
  @MockBean ZoneDetectionJobService zoneDetectionJobServiceMock;
  @MockBean EventProducer eventProducerMock;
  public static final String MOCK_JOB_ID = "mock_job_id";
  public static final String MOCK_HUMAN_JOB_ID = "mock_human_job_id";
  private Feature feature;
  private final List<MachineDetectedTile> machineDetectedTiles =
      List.of(
          differentDetectedTile(List.of()),
          differentDetectedTile(
              List.of(
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(SOLAR_PANEL))),
          differentDetectedTile(
              List.of(
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(SOLAR_PANEL))));

  public static MachineDetectedTile differentDetectedTile(List<DetectedObject> detectedObjects) {
    return MachineDetectedTile.builder()
        .id(randomUUID().toString())
        .bucketPath(LAYER_20_10_1_PNG + randomUUID())
        .tile(Tile.builder().build())
        .zdjJobId(MOCK_JOB_ID)
        .parcelId(PARCEL_MOCK_ID)
        .creationDatetime(Instant.now())
        .detectedObjects(detectedObjects)
        .build();
  }

  @SneakyThrows
  DetectedObject inDoubtDetectedObject(DetectableType type) {
    String id = randomUUID().toString();
    return DetectedObject.builder()
        .id(id)
        .detectedObjectType(detectedObjectType(id, type))
        .feature(feature)
        .computedConfidence(0.75)
        .build();
  }

  private static DetectableObjectType detectedObjectType(String id, DetectableType type) {
    return DetectableObjectType.builder().id(id).objectId(id).detectableType(type).build();
  }

  void setupDetectedTileRepository(DetectedTileRepository detectedTileRepository) {
    when(detectedTileRepository.findAllByZdjJobId(MOCK_JOB_ID)).thenReturn(machineDetectedTiles);
  }

  void setUpObjectConfigurationRepository(
      DetectableObjectConfigurationRepository objectConfigurationRepositoryMock) {
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(MOCK_JOB_ID))
        .thenReturn(
            List.of(
                DetectableObjectConfiguration.builder().objectType(ROOF).confidence(0.8).build(),
                DetectableObjectConfiguration.builder().objectType(TREE).confidence(0.8).build(),
                DetectableObjectConfiguration.builder()
                    .objectType(SOLAR_PANEL)
                    .confidence(0.8)
                    .build()));
  }

  @BeforeEach
  void setup() throws JsonProcessingException {
    setupDetectedTileRepository(detectedTileRepositoryMock);
    setUpObjectConfigurationRepository(objectConfigurationRepositoryMock);
    when(humanDetectionJobRepositoryMock.save(any()))
        .thenReturn(HumanDetectionJob.builder().build());

    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(MOCK_JOB_ID))
        .thenReturn(ZoneDetectionJob.builder().id(MOCK_HUMAN_JOB_ID).build());
    feature = om.readValue(MOCK_FEATURE_AS_STRING, Feature.class);
  }

  @Test
  void accept_event_ok() throws ApiException {
    String annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    String annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    String annotationJobWithoutObjectsId = randomUUID().toString();
    subject.accept(
        MOCK_JOB_ID,
        DEFAULT_MIN_CONFIDENCE,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);

    verify(annotationServiceMock, times(2)).createAnnotationJob(any(), any());
  }
}
