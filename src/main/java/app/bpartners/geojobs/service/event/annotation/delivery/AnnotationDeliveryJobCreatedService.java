package app.bpartners.geojobs.service.event.annotation.delivery;

import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobCreated;
import app.bpartners.geojobs.service.AnnotationDeliveryJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationDeliveryJobCreatedService implements Consumer<AnnotationDeliveryJobCreated> {
  private final AnnotationDeliveryJobService AnnotationDeliveryJobService;

  @Override
  public void accept(AnnotationDeliveryJobCreated event) {
    AnnotationDeliveryJobService.fireTasks(event.getDeliveryJob().getId());
  }
}
