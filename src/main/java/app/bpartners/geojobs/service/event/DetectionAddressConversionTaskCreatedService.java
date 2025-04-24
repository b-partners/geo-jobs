package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionTaskCreated;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.DetectionAddressConversionTaskRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.TaskConsumer;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class DetectionAddressConversionTaskCreatedService
    extends TaskCreatedService<
        DetectionAddressConversionTask, DetectionAddressConversionTaskCreated>
    implements Consumer<DetectionAddressConversionTaskCreated> {
  public DetectionAddressConversionTaskCreatedService(
      TaskConsumer<DetectionAddressConversionTask> taskConsumer,
      TaskStatusService<DetectionAddressConversionTask> taskStatusService,
      DetectionAddressConversionTaskRepository taskRepository) {
    super(taskConsumer, taskStatusService, taskRepository);
  }

  @Override
  public void accept(DetectionAddressConversionTaskCreated event) {
    event.getTask().setE2ApiKey(event.getE2ApiKey());
    super.accept(event);
  }
}
