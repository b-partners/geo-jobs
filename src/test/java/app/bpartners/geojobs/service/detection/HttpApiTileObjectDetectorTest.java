package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_NOIRCIE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.VELUX;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.repository.DetectionFileObjectRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class HttpApiTileObjectDetectorTest {
  private static final String BUCKET_NAME = "dummyBucketName";
  private static final String TILE_BUCKET_PATH = "dummyTileBucketPath";
  private static final String V1_API_URL = "https://v1.detection.test";
  private static final String FIRST_V2_API_URL = "https://first-v2.detection.test";
  private static final String SECOND_V2_API_URL = "https://second-v2.detection.test";

  private final CustomBucketComponent customBucketComponentMock = mock();
  private final TileObjectDetectorConf tileObjectDetectorConfMock = mock();
  private final DetectionFileObjectRepository detectionFileObjectRepositoryMock = mock();
  private final BucketComponent bucketComponentMock = mock();
  private final FileWriter fileWriterMock = mock();
  private final RestTemplate restTemplateMock = mock();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  private final HttpApiTileObjectDetector subject =
      new HttpApiTileObjectDetector(
          objectMapper,
          customBucketComponentMock,
          "https://default.detection.test",
          tileObjectDetectorConfMock,
          new DetectionResponseAggregator(),
          new DetectionResponseAggregatorV1(),
          new DetectionResponseV1ToV2Mapper(),
          detectionFileObjectRepositoryMock,
          bucketComponentMock,
          fileWriterMock,
          objectMapper,
          restTemplateMock);

  @SneakyThrows
  @Test
  void calls_all_detection_apis_concurrently() {
    setUpTile();
    var concurrentCallsLatch = new CountDownLatch(3);
    var calledUrls = ConcurrentHashMap.<String>newKeySet();
    answerOnceEveryApiIsCalled(concurrentCallsLatch, calledUrls);

    assertDoesNotThrow(() -> subject.apply(tileDetectionTask(), null, detectableObjectConfs()));

    assertEquals(Set.of(V1_API_URL, FIRST_V2_API_URL, SECOND_V2_API_URL), calledUrls);
  }

  @SneakyThrows
  @Test
  void aggregates_every_api_response_whatever_their_completion_order() {
    setUpTile();
    var concurrentCallsLatch = new CountDownLatch(3);
    answerOnceEveryApiIsCalled(concurrentCallsLatch, ConcurrentHashMap.newKeySet());

    var actual = subject.apply(tileDetectionTask(), null, detectableObjectConfs());

    assertEquals(3, actual.getImages().size());
    assertTrue(
        actual.getImages().values().stream()
            .allMatch(imageData -> imageData.getRegions().size() == 1));
    verify(restTemplateMock, times(1))
        .postForEntity(eq(V1_API_URL), any(), eq(DetectionResponse.class));
    verify(restTemplateMock, times(2))
        .postForEntity(anyString(), any(), eq(DetectionResponseV2.class));
  }

  private void answerOnceEveryApiIsCalled(CountDownLatch latch, Set<String> calledUrls) {
    when(restTemplateMock.postForEntity(anyString(), any(), eq(DetectionResponseV2.class)))
        .thenAnswer(
            invocation -> {
              String apiUrl = invocation.getArgument(0);
              calledUrls.add(apiUrl);
              awaitEveryOtherCall(latch);
              return new ResponseEntity<>(v2Response(apiUrl), HttpStatus.OK);
            });
    when(restTemplateMock.postForEntity(anyString(), any(), eq(DetectionResponse.class)))
        .thenAnswer(
            invocation -> {
              String apiUrl = invocation.getArgument(0);
              calledUrls.add(apiUrl);
              awaitEveryOtherCall(latch);
              return new ResponseEntity<>(v1Response(apiUrl), HttpStatus.OK);
            });
  }

  @SneakyThrows
  private static void awaitEveryOtherCall(CountDownLatch latch) {
    latch.countDown();
    if (!latch.await(10, SECONDS)) {
      throw new IllegalStateException("detection apis were called sequentially");
    }
  }

  private DetectionResponseV2 v2Response(String apiUrl) {
    return DetectionResponseV2.builder()
        .images(
            new java.util.HashMap<>(
                java.util.Map.of(
                    apiUrl,
                    DetectionResponseV2.ImageData.builder()
                        .filename(apiUrl)
                        .regions(new java.util.HashMap<>(java.util.Map.of("region_1", region())))
                        .build())))
        .build();
  }

  private DetectionResponseV2.ImageData.Region region() {
    return DetectionResponseV2.ImageData.Region.builder()
        .regionAttributes(new java.util.HashMap<>())
        .shapeAttributes(
            DetectionResponseV2.ImageData.ShapeAttributes.builder()
                .name("polygon")
                .allPointsX(List.of(java.math.BigDecimal.ZERO))
                .allPointsY(List.of(java.math.BigDecimal.ZERO))
                .build())
        .build();
  }

  private DetectionResponse v1Response(String apiUrl) {
    return DetectionResponse.builder()
        .rstRaw(
            new java.util.HashMap<>(
                java.util.Map.of(
                    apiUrl,
                    DetectionResponse.ImageData.builder()
                        .filename(apiUrl)
                        .regions(new java.util.HashMap<>(java.util.Map.of("region_1", v1Region())))
                        .build())))
        .build();
  }

  private DetectionResponse.ImageData.Region v1Region() {
    return DetectionResponse.ImageData.Region.builder()
        .regionAttributes(new java.util.HashMap<>())
        .shapeAttributes(
            DetectionResponse.ImageData.ShapeAttributes.builder()
                .name("polygon")
                .allPointsX(List.of(java.math.BigDecimal.ZERO))
                .allPointsY(List.of(java.math.BigDecimal.ZERO))
                .build())
        .build();
  }

  @SneakyThrows
  private void setUpTile() {
    var bucketConfMock = mock(BucketConf.class);
    var tileImageFile = File.createTempFile("tile", ".jpg");
    Files.write(tileImageFile.toPath(), new byte[] {1, 2, 3});
    tileImageFile.deleteOnExit();

    when(bucketConfMock.getBucketName()).thenReturn(BUCKET_NAME);
    when(customBucketComponentMock.getBucketConf()).thenReturn(bucketConfMock);
    when(customBucketComponentMock.download(BUCKET_NAME, TILE_BUCKET_PATH))
        .thenReturn(tileImageFile);
    when(tileObjectDetectorConfMock.getTileDetectionApiUrls()).thenReturn(tileDetectionApiUrls());
  }

  private TileDetectionTask tileDetectionTask() {
    return TileDetectionTask.builder()
        .id(randomUUID().toString())
        .jobId(randomUUID().toString())
        .detectionIdentifier(randomUUID().toString())
        .tile(
            Tile.builder()
                .id(randomUUID().toString())
                .bucketPath(TILE_BUCKET_PATH)
                .coordinates(new TileCoordinates().x(1).y(1).z(20))
                .build())
        .build();
  }

  private List<DetectableObjectConfiguration> detectableObjectConfs() {
    return List.of(
        detectableObjectConf(PISCINE),
        detectableObjectConf(VELUX),
        detectableObjectConf(MOISISSURE_NOIRCIE));
  }

  private DetectableObjectConfiguration detectableObjectConf(DetectableType objectType) {
    return DetectableObjectConfiguration.builder()
        .id(randomUUID().toString())
        .objectType(objectType)
        .build();
  }

  private String tileDetectionApiUrls() {
    return """
           [
             {"objectType": "PISCINE", "url": "%s", "version": "V1"},
             {"objectType": "VELUX", "url": "%s", "version": "V2"},
             {"objectType": "MOISISSURE_NOIRCIE", "url": "%s", "version": "V2"}
           ]"""
        .formatted(V1_API_URL, FIRST_V2_API_URL, SECOND_V2_API_URL);
  }
}
