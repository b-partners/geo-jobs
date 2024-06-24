package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.MockedTileObjectDetector;
import java.util.List;
import org.junit.jupiter.api.Test;

class TileParcelParcelDetectionTaskConsumerWithMockedObjectsDetectorTest {

  @Test
  void can_consume_with_no_error() {
    DetectedTileRepository detectedTileRepositoryMock = mock();
    DetectionMapper detectionMapperMock = mock();
    when(detectedTileRepositoryMock.save(any())).thenReturn(new DetectedTile());
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new DetectedTile());
    var subject =
        new TileDetectionTaskCreatedConsumer(
            detectedTileRepositoryMock, new MockedTileObjectDetector(), detectionMapperMock);

    subject.accept(
        new TileDetectionTaskCreated(
            "zdjId",
            new TileDetectionTask(
                "tileDetectionTaskId",
                "detectionTaskId",
                "parcelId",
                "jobId",
                Tile.builder().coordinates(new TileCoordinates().z(20).x(0).y(0)).build(),
                List.of()),
            List.of()));
  }

  @Test
  void can_consume_with_some_errors() {
    DetectedTileRepository detectedTileRepositoryMock = mock();
    DetectionMapper detectionMapperMock = mock();
    when(detectedTileRepositoryMock.save(any())).thenReturn(new DetectedTile());
    var subject =
        new TileDetectionTaskCreatedConsumer(
            detectedTileRepositoryMock, new MockedTileObjectDetector(), detectionMapperMock);

    try {
      for (int i = 0; i < 10; i++) {
        subject.accept(
            new TileDetectionTaskCreated(
                "zdjId",
                new TileDetectionTask(
                    "tileDetectionTaskId", "taskId", "parcelId", "jobId", new Tile(), List.of()),
                List.of()));
      }
    } catch (Exception e) {
      return;
    }
    fail();
  }
}
