package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TileParcelParcelDetectionTaskCreatedConsumerTest {
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  TileObjectDetector objectDetectorMock = mock();
  DetectionMapper detectionMapperMock = mock();
  TileDetectionTaskCreatedConsumer subject =
      new TileDetectionTaskCreatedConsumer(
          machineDetectedTileRepositoryMock, objectDetectorMock, detectionMapperMock);

  @Test
  void accept_ok() {
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(new MachineDetectedTile());
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(new DetectionResponse());
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new TileDetectionTaskCreated(
                    "zdjId",
                    TileDetectionTask.builder().build(),
                    List.of(
                        DetectableObjectConfiguration.builder()
                            .objectType(PASSAGE_PIETON)
                            .build()))));

    var detectedTileCaptor = ArgumentCaptor.forClass(MachineDetectedTile.class);
    verify(machineDetectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
  }
}
