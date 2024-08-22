package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ParcelDetectionTaskCreatedService implements Consumer<ParcelDetectionTaskCreated> {
  private final TaskStatusService<ParcelDetectionTask> taskStatusService;
  private final ParcelDetectionTaskConsumer parcelDetectionTaskConsumer;

  @Override
  public void accept(ParcelDetectionTaskCreated parcelDetectionTaskCreated) {
    var task = parcelDetectionTaskCreated.getTask();
    taskStatusService.process(task);

    parcelDetectionTaskConsumer.accept(task);
  }
}
