package school.hei.geojobs.service.event;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.geojobs.endpoint.event.gen.ZoneDetectionTaskCreated;
import school.hei.geojobs.model.exception.ApiException;
import school.hei.geojobs.repository.DetectedTileRepository;
import school.hei.geojobs.repository.model.DetectedTile;
import school.hei.geojobs.service.ZoneDetectionTaskStatusService;
import school.hei.geojobs.service.geo.TilesDetectionApi;
import school.hei.geojobs.service.geo.response.DetectionResponse;
import school.hei.geojobs.service.mapper.DetectionMapper;

@Service
@AllArgsConstructor
public class ZoneDetectionTaskCreatedService implements Consumer<ZoneDetectionTaskCreated> {
  private final TilesDetectionApi tilesDetectionApi;
  private final ZoneDetectionTaskStatusService zoneDetectionTaskStatusService;
  private final DetectedTileRepository detectedTileRepository;

  @Override
  public void accept(ZoneDetectionTaskCreated zoneDetectionTaskCreated) {
    var task = zoneDetectionTaskCreated.getTask();
    zoneDetectionTaskStatusService.process(task);
    try {
      DetectionResponse response = tilesDetectionApi.detect(task);
      DetectedTile detectedTile = DetectionMapper.toDetectedTile(response, task.getTile());
      detectedTileRepository.save(detectedTile);
      zoneDetectionTaskStatusService.succeed(task);
    } catch (RuntimeException e) {
      zoneDetectionTaskStatusService.fail(task);
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
