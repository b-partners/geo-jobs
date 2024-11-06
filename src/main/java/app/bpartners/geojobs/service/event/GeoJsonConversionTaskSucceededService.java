package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskSucceeded;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionTaskSucceededService
    implements Consumer<GeoJsonConversionTaskSucceeded> {
  private final TaskStatusService<GeoJsonConversionTask> taskStatusService;
  private final GeoJsonConversionTaskRepository conversionTaskRepository;

  @Override
  public void accept(GeoJsonConversionTaskSucceeded geoJsonConversionTaskSucceeded) {
    var task = geoJsonConversionTaskSucceeded.getGeoJsonConversionTask();
    conversionTaskRepository.save(task);
    taskStatusService.succeed(task);
  }
}
