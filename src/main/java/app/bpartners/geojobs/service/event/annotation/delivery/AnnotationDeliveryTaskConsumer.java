package app.bpartners.geojobs.service.event.annotation.delivery;

import static java.time.Instant.now;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryTask;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AnnotationDeliveryTaskConsumer implements Consumer<AnnotationDeliveryTask> {
  @Override
  public void accept(AnnotationDeliveryTask task) {
    throw new NotImplementedException("Not supported for now");
  }

  public static AnnotationDeliveryTask withNewStatus(
      AnnotationDeliveryTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (AnnotationDeliveryTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
