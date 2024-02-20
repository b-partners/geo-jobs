package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.conf.EnvConf.ANNOTATOR_USER_ID_FOR_GEOJOBS;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static app.bpartners.geojobs.service.event.TilingTaskCreatedServiceIT.MOCK_FEATURE_AS_STRING;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.annotator.endpoint.rest.api.AnnotatedJobsApi;
import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  public static final String LAYER_20_10_1_PNG = "layer/20/10/1.png";
  @Autowired ZoneDetectionJobSucceededService subject;
  @Autowired private ObjectMapper om;
  @MockBean DetectedTileRepository detectedTileRepositoryMock;
  @MockBean DetectableObjectConfigurationRepository objectConfigurationRepositoryMock;
  AnnotatedJobsApi annotatorApiClientMock = mock(AnnotatedJobsApi.class);
  @MockBean BucketComponent selfBucketComponentMock;
  @MockBean TaskExtractor taskExtractorMock;
  @MockBean LabelExtractor labelExtractorMock;
  public static final String MOCK_JOB_ID = "mock_job_id";
  private Feature feature;
  private final List<DetectedTile> detectedTiles =
      List.of(
          detectedTile(
              List.of(
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(SOLAR_PANEL))),
          detectedTile(
              List.of(
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(TREE),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(ROOF),
                  inDoubtDetectedObject(SOLAR_PANEL))));

  public static DetectedTile detectedTile(List<DetectedObject> detectedObjects) {
    return DetectedTile.builder()
        .id(randomUUID().toString())
        .bucketPath(LAYER_20_10_1_PNG)
        .tile(Tile.builder().build())
        .jobId(MOCK_JOB_ID)
        .creationDatetime(Instant.now())
        .detectedObjects(detectedObjects)
        .build();
  }

  @SneakyThrows
  DetectedObject inDoubtDetectedObject(DetectableType type) {
    String id = randomUUID().toString();
    return DetectedObject.builder()
        .id(id)
        .detectedObjectTypes(detectedObjectType(id, type))
        .feature(feature)
        .computedConfidence(0.75)
        .build();
  }

  private static List<DetectableObjectType> detectedObjectType(String id, DetectableType type) {
    return List.of(DetectableObjectType.builder().objectId(id).detectableType(type).build());
  }

  void setupDetectedTileRepository(DetectedTileRepository detectedTileRepository) {
    when(detectedTileRepository.findAllByJobId(MOCK_JOB_ID)).thenReturn(detectedTiles);
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
    feature = om.readValue(MOCK_FEATURE_AS_STRING, Feature.class);
    subject = subject.annotatedJobsApi(annotatorApiClientMock);
  }

  @Test
  void accept_event_ok() {
    subject.accept(ZoneDetectionJobSucceeded.builder().jobId(MOCK_JOB_ID).build());

    verify(taskExtractorMock, times(1)).apply(detectedTiles, ANNOTATOR_USER_ID_FOR_GEOJOBS);
    verify(labelExtractorMock, times(1)).extractLabelsFromTasks(anyList());
  }
}
