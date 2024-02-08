package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.geo.detection.DetectedTile;
import app.bpartners.geojobs.service.geo.detection.DetectionMapper;
import app.bpartners.geojobs.service.geo.detection.DetectionResponse;
import app.bpartners.geojobs.service.geo.detection.DetectionTaskStatusService;
import app.bpartners.geojobs.service.geo.detection.ObjectsDetector;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionTaskCreatedService implements Consumer<DetectionTaskCreated> {
  private final ObjectsDetector objectsDetector;
  private final DetectionTaskStatusService detectionTaskStatusService;
  private final DetectedTileRepository detectedTileRepository;

  @Override
  public void accept(DetectionTaskCreated detectionTaskCreated) {
    var task = detectionTaskCreated.getTask();
    detectionTaskStatusService.process(task);
    try {
      DetectionResponse response = objectsDetector.apply(task);
      DetectedTile detectedTile =
          DetectionMapper.toDetectedTile(response, task.getTile(), task.getJobId());
      detectedTileRepository.save(detectedTile);
      detectionTaskStatusService.succeed(task);
    } catch (RuntimeException e) {
      detectionTaskStatusService.fail(task);
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
