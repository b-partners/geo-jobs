package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskCreatedFailed;
import app.bpartners.geojobs.endpoint.event.model.tile.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.service.detection.TileDetectionTaskStatusService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class TileDetectionTaskCreatedServiceTest {
  TileDetectionTaskStatusService tileDetectionTaskStatusServiceMock = mock();
  TileDetectionTaskCreatedConsumer tileDetectionTaskConsumerMock = mock();
  EventProducer eventProducerMock = mock();
  TileDetectionTaskCreatedService subject =
      new TileDetectionTaskCreatedService(
          tileDetectionTaskStatusServiceMock,
          tileDetectionTaskConsumerMock,
          eventProducerMock,
          new ExceptionToStringFunction());

  @Test
  void accept_ok() {
    doNothing().when(tileDetectionTaskConsumerMock).accept(any());
    when(tileDetectionTaskStatusServiceMock.process(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
                .build());
    when(tileDetectionTaskStatusServiceMock.succeed(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                .build());

    assertDoesNotThrow(
        () ->
            subject.accept(
                new TileDetectionTaskCreated(
                    "zdjId",
                    TileDetectionTask.builder()
                        .statusHistory(
                            List.of(
                                TaskStatus.builder()
                                    .progression(FINISHED)
                                    .health(SUCCEEDED)
                                    .build()))
                        .build(),
                    List.of(DetectableObjectConfiguration.builder().objectType(PATHWAY).build()))));
    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    TileDetectionTaskSucceeded capturedTileDetectionTaskSucceeded =
        (TileDetectionTaskSucceeded) eventCaptor.getValue().getFirst();
    TileDetectionTask tileDetectionTask = capturedTileDetectionTaskSucceeded.getTileDetectionTask();
    assertEquals(FINISHED, tileDetectionTask.getStatus().getProgression());
    assertEquals(SUCCEEDED, tileDetectionTask.getStatus().getHealth());
  }

  @Test
  void accept_ko() {
    doThrow(ApiException.class).when(tileDetectionTaskConsumerMock).accept(any());
    when(tileDetectionTaskStatusServiceMock.process(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(PROCESSING).health(UNKNOWN).build()))
                .build());
    when(tileDetectionTaskStatusServiceMock.succeed(any()))
        .thenReturn(
            TileDetectionTask.builder()
                .statusHistory(
                    List.of(TaskStatus.builder().progression(FINISHED).health(SUCCEEDED).build()))
                .build());
    TileDetectionTaskCreated expectedTileDetectionTaskCreated =
        new TileDetectionTaskCreated(
            "zdjId",
            TileDetectionTask.builder().build(),
            List.of(DetectableObjectConfiguration.builder().objectType(PATHWAY).build()));
    assertDoesNotThrow(() -> subject.accept(expectedTileDetectionTaskCreated));
    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    TileDetectionTaskCreatedFailed capturedTileDetectionTaskCreatedFailed =
        (TileDetectionTaskCreatedFailed) eventCaptor.getValue().getFirst();
    TileDetectionTaskCreated capturedTileDetectionTaskCreated =
        capturedTileDetectionTaskCreatedFailed.getTileDetectionTaskCreated();
    assertEquals(expectedTileDetectionTaskCreated, capturedTileDetectionTaskCreated);
  }
}
