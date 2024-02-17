package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.job.service.RetryableTaskStatusService;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.ObjectsDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionTaskCreatedService implements Consumer<DetectionTaskCreated> {
  private final ObjectsDetector objectsDetector;
  private final RetryableTaskStatusService<DetectionTask, ZoneDetectionJob> taskStatusService;
  private final DetectedTileRepository detectedTileRepository;

  @Override
  public void accept(DetectionTaskCreated detectionTaskCreated) {
    var task = detectionTaskCreated.getTask();
    taskStatusService.process(task);

    try {
      DetectionResponse response = objectsDetector.apply(task);
      DetectedTile detectedTile =
          DetectionMapper.toDetectedTile(response, task.getTile(), task.getJobId());
      detectedTileRepository.save(detectedTile);
      taskStatusService.succeed(task);
    } catch (RuntimeException e) {
      taskStatusService.fail(task);
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
