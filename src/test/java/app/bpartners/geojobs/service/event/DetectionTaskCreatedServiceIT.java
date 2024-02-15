package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.self.BucketComponent;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ObjectsDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class DetectionTaskCreatedServiceIT extends FacadeIT {
  private static final String FILE_NAME =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "mockData"
          + File.separator
          + "image-to-detect.jpg";

  @Autowired DetectionTaskCreatedService subject;

  @MockBean ObjectsDetector objectsDetector;

  @MockBean BucketComponent bucketComponent;

  @MockBean DetectedTileRepository detectedTileRepository;

  @MockBean DetectionTaskRepository detectionTaskRepository;

  @MockBean ZoneDetectionJobService zoneDetectionJobService;

  @Captor ArgumentCaptor<DetectedTile> detectedTileCaptor;

  DetectionResponse detectionResponse() {
    return DetectionResponse.builder()
        .rstImageUrl("mock-rst-s3-url")
        .srcImageUrl("mock-src-s3-url")
        .rstRaw(
            Map.of(
                "filename",
                DetectionResponse.ImageData.builder()
                    .regions(
                        Map.of(
                            "0",
                            DetectionResponse.ImageData.Region.builder()
                                .shapeAttributes(
                                    DetectionResponse.ImageData.ShapeAttributes.builder()
                                        .allPointsX(
                                            List.of(
                                                new BigDecimal("210.6243386243386"),
                                                new BigDecimal("216.38095238095238"),
                                                new BigDecimal("223.83068783068782")))
                                        .allPointsY(
                                            List.of(
                                                new BigDecimal("220.78306878306876"),
                                                new BigDecimal("195.38624338624336"),
                                                new BigDecimal("210.6243386243386")))
                                        .build())
                                .regionAttributes(Map.of("label", "roof", "confidence", "0.8436"))
                                .build(),
                            "1",
                            DetectionResponse.ImageData.Region.builder()
                                .shapeAttributes(
                                    DetectionResponse.ImageData.ShapeAttributes.builder()
                                        .allPointsX(
                                            List.of(
                                                new BigDecimal("210.6243386243386"),
                                                new BigDecimal("216.38095238095238"),
                                                new BigDecimal("223.83068783068782")))
                                        .allPointsY(
                                            List.of(
                                                new BigDecimal("220.78306878306876"),
                                                new BigDecimal("195.38624338624336"),
                                                new BigDecimal("210.6243386243386")))
                                        .build())
                                .regionAttributes(Map.of("label", "roof", "confidence", "0.9612"))
                                .build()))
                    .build()))
        .build();
  }

  DetectionTaskCreated detectionTaskCreated() {
    String taskId = randomUUID().toString();
    String jobId = randomUUID().toString();

    return DetectionTaskCreated.builder()
        .task(
            DetectionTask.builder()
                .id(taskId)
                .jobId(jobId)
                .tile(
                    Tile.builder()
                        .id(randomUUID().toString())
                        .coordinates(new TileCoordinates().x(25659).y(15466).z(20))
                        .bucketPath("mock-bucket-key")
                        .build())
                .statusHistory(
                    new ArrayList<>() {
                      {
                        TaskStatus.builder()
                            .id(randomUUID().toString())
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .jobType(DETECTION)
                            .taskId(taskId)
                            .creationDatetime(now())
                            .build();
                      }
                    })
                .build())
        .build();
  }

  ZoneDetectionJob zoneDetectionJob() {
    var task = detectionTaskCreated().getTask();
    String jobId = task.getJobId();
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("mock")
        .emailReceiver("mock@gmail.com")
        .build();
  }

  @Test
  void process_detection() {
    when(bucketComponent.download(any())).thenReturn(new File(FILE_NAME));
    when(objectsDetector.apply(any())).thenReturn(detectionResponse());
    when(detectionTaskRepository.existsById(any())).thenReturn(true);
    when(zoneDetectionJobService.findById(any())).thenReturn(zoneDetectionJob());
    when(zoneDetectionJobService.pwFindById(any())).thenReturn(zoneDetectionJob());

    subject.accept(detectionTaskCreated());

    verify(detectedTileRepository).save(detectedTileCaptor.capture());

    DetectedTile detectedTile = detectedTileCaptor.getValue();

    List<DetectedObject> actualObjects = detectedTile.getDetectedObjects();
    assertNotNull(detectedTile.getId());
    assertNotNull(detectedTile.getTile());
    assertNotNull(actualObjects);
    assertFalse(actualObjects.isEmpty());
    assertTrue(
        actualObjects.stream()
            .allMatch(
                detectedObject ->
                    detectedObject.getComputedConfidence() != null
                        && detectedObject.getComputedConfidence() > 0));
    assertFalse(actualObjects.get(0).getFeature().getGeometry().getCoordinates().isEmpty());
  }
}
