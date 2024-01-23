package school.hei.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static school.hei.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static school.hei.geojobs.repository.model.Status.ProgressionStatus.PENDING;

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
import school.hei.geojobs.conf.FacadeIT;
import school.hei.geojobs.endpoint.event.gen.ZoneDetectionTaskCreated;
import school.hei.geojobs.endpoint.rest.model.TileCoordinates;
import school.hei.geojobs.file.BucketComponent;
import school.hei.geojobs.repository.DetectedTileRepository;
import school.hei.geojobs.repository.ZoneDetectionTaskRepository;
import school.hei.geojobs.repository.model.DetectedTile;
import school.hei.geojobs.repository.model.DetectionJobStatus;
import school.hei.geojobs.repository.model.DetectionTaskStatus;
import school.hei.geojobs.repository.model.Tile;
import school.hei.geojobs.repository.model.ZoneDetectionJob;
import school.hei.geojobs.repository.model.ZoneDetectionTask;
import school.hei.geojobs.service.ZoneDetectionJobService;
import school.hei.geojobs.service.geo.TilesDetectionApi;
import school.hei.geojobs.service.geo.response.DetectionResponse;

@Slf4j
class ZoneDetectionTaskCreatedServiceIT extends FacadeIT {
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

  @Autowired ZoneDetectionTaskCreatedService subject;

  @MockBean TilesDetectionApi tilesDetectionApi;

  @MockBean BucketComponent bucketComponent;

  @MockBean DetectedTileRepository detectedTileRepository;

  @MockBean ZoneDetectionTaskRepository zoneDetectionTaskRepository;

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
                                .regionAttributes(Map.of("label", "roof"))
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
                                .regionAttributes(Map.of("label", "roof"))
                                .build()))
                    .build()))
        .build();
  }

  ZoneDetectionTaskCreated zoneDetectionTaskCreated() {
    String taskId = randomUUID().toString();
    String jobId = randomUUID().toString();

    return ZoneDetectionTaskCreated.builder()
        .task(
            ZoneDetectionTask.builder()
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
                        DetectionTaskStatus.builder()
                            .id(randomUUID().toString())
                            .progression(PENDING)
                            .health(UNKNOWN)
                            .taskId(taskId)
                            .creationDatetime(now())
                            .build();
                      }
                    })
                .build())
        .build();
  }

  ZoneDetectionJob zoneDetectionJob() {
    var task = zoneDetectionTaskCreated().getTask();
    String jobId = task.getJobId();
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("mock")
        .emailReceiver("mock@gmail.com")
        .tasks(List.of(task))
        .statusHistory(
            new ArrayList<>() {
              {
                DetectionJobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(PENDING)
                    .health(UNKNOWN)
                    .build();
              }
            })
        .build();
  }

  @Test
  void process_detection() {
    when(bucketComponent.download(any())).thenReturn(new File(FILE_NAME));
    when(tilesDetectionApi.detect(any())).thenReturn(detectionResponse());
    when(zoneDetectionTaskRepository.existsById(any())).thenReturn(true);
    when(zoneDetectionJobService.findById(any())).thenReturn(zoneDetectionJob());

    subject.accept(zoneDetectionTaskCreated());

    verify(detectedTileRepository).save(detectedTileCaptor.capture());

    DetectedTile detectedTile = detectedTileCaptor.getValue();

    assertNotNull(detectedTile.getId());
    assertNotNull(detectedTile.getTile());
    assertNotNull(detectedTile.getDetectedObjects());
    assertFalse(detectedTile.getDetectedObjects().isEmpty());
    assertFalse(
        detectedTile
            .getDetectedObjects()
            .get(0)
            .getFeature()
            .getGeometry()
            .getCoordinates()
            .isEmpty());
  }
}
