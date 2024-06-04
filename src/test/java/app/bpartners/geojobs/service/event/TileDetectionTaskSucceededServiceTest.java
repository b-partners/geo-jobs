package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskAsJobStatusService;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import org.junit.jupiter.api.Test;

public class TileDetectionTaskSucceededServiceTest {
  TaskAsJobStatusService<TileDetectionTask, DetectionTask> taskAsJobStatusServiceMock = mock();
  TileDetectionTaskRepository tileDetectionTaskRepositoryMock = mock();
  TileDetectionTaskSucceededService subject =
      new TileDetectionTaskSucceededService(
          taskAsJobStatusServiceMock, tileDetectionTaskRepositoryMock);

  @Test
  void consume_with_any_error_ok() {
    when(tileDetectionTaskRepositoryMock.save(any())).thenReturn(new TileDetectionTask());
    when(taskAsJobStatusServiceMock.succeed(any())).thenReturn(new TileDetectionTask());

    assertDoesNotThrow(
        () -> subject.accept(new TileDetectionTaskSucceeded(new TileDetectionTask())));
  }
}
