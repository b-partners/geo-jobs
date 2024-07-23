package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.unit.GeoJsonMapperTest.detectedObject;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionInitiated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.repository.model.GeoJsonConversionTask;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionTaskStatusService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class GeoJsonConversionInitiatedServiceIT extends FacadeIT {
  private static final String MOCK_JOB_ID = "mock_job_id";
  private static final String MOCK_TASK_ID = "mock_task_id";
  @MockBean HumanDetectedTileService humanDetectedTileService;
  @MockBean ZoneDetectionJobService zoneDetectionJobService;
  @Autowired GeoJsonConversionTaskStatusService taskStatusService;
  @Autowired GeoJsonConversionTaskService taskService;
  @Autowired GeoJsonConverter geoJsonConverter;
  @Autowired FileWriter writer;
  @MockBean BucketComponent bucketComponent;
  @Autowired GeoJsonConversionInitiatedService subject;

  ZoneDetectionJob detectionJob() {
    return ZoneDetectionJob.builder().zoneName("Cannes").build();
  }

  GeoJsonConversionTask conversionTask() {
    return GeoJsonConversionTask.builder()
        .id(MOCK_TASK_ID)
        .jobId(MOCK_JOB_ID)
        .submissionInstant(now())
        .fileKey(null)
        .statusHistory(List.of())
        .build();
  }

  HumanDetectedTile humanDetectedTile() {
    return HumanDetectedTile.builder()
        .id(randomUUID().toString())
        .annotationTaskId(MOCK_TASK_ID)
        .imageSize(1024)
        .tile(Tile.builder().coordinates(new TileCoordinates().x(521151).y(151151).z(20)).build())
        .detectedObjects(List.of(detectedObject()))
        .build();
  }

  GeoJsonConversionInitiated initiated() {
    return new GeoJsonConversionInitiated(MOCK_JOB_ID, MOCK_TASK_ID, detectionJob().getZoneName());
  }

  @BeforeEach
  void setUp() throws MalformedURLException {
    when(zoneDetectionJobService.getHumanZdjFromZdjId(MOCK_JOB_ID)).thenReturn(detectionJob());
    when(humanDetectedTileService.getByJobId(MOCK_JOB_ID)).thenReturn(List.of(humanDetectedTile()));
    when(bucketComponent.presign(any(), any()))
        .thenReturn(new URL("https://s3presignedurl.aws.com"));
  }

  @Test
  void generate_geo_json_from_detected_tiles() {
    taskService.save(conversionTask());

    subject.accept(initiated());
    var actual = taskService.getById(MOCK_TASK_ID);

    assertEquals("mock_job_id/Cannes.geojson", actual.getFileKey());
    assertEquals(FINISHED, actual.getStatus().getProgression());
    assertEquals(SUCCEEDED, actual.getStatus().getHealth());
  }
}
