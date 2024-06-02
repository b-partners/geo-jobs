package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TileDetectionTaskCreatedConsumerTest {
  DetectedTileRepository detectedTileRepositoryMock = mock();
  TileObjectDetector objectDetectorMock = mock();
  DetectionMapper detectionMapperMock = mock();
  TileDetectionTaskCreatedConsumer subject =
      new TileDetectionTaskCreatedConsumer(
          detectedTileRepositoryMock, objectDetectorMock, detectionMapperMock);

  @Test
  void accept_ok() {
    when(detectedTileRepositoryMock.save(any())).thenReturn(new DetectedTile());
    when(objectDetectorMock.apply(any(), any())).thenReturn(new DetectionResponse());
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any()))
        .thenReturn(new DetectedTile());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new TileDetectionTaskCreated(
                    TileDetectionTask.builder().build(), List.of(DetectableType.PATHWAY))));

    var detectedTileCaptor = ArgumentCaptor.forClass(DetectedTile.class);
    verify(detectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
  }
}
