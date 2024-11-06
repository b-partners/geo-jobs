package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskCreated;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionTaskCreatedService implements Consumer<GeoJsonConversionTaskCreated> {
  private final TaskStatusService<GeoJsonConversionTask> taskStatusService;
  private final GeoJsonConversionTaskConsumer geoJsonConversionTaskConsumer;
  private final EventProducer eventProducer;

  @Override
  public void accept(GeoJsonConversionTaskCreated geoJsonConversionTaskCreated) {
    var task = geoJsonConversionTaskCreated.getGeoJsonConversionTask();
    taskStatusService.process(task);

    geoJsonConversionTaskConsumer.accept(task);
  }
}
