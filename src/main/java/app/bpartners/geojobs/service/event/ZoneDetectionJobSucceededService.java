package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ZoneDetectionJobSucceededService implements Consumer<ZoneDetectionJobSucceeded> {
  public static final double DEFAULT_MINIMUM_CONFIDENCE_FOR_DELIVERY = 0.95;
  private final EventProducer eventProducer;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();

    eventProducer.accept(
        List.of(
            AnnotationDeliveryJobRequested.builder()
                .jobId(event.getSucceededJobId())
                .minimumConfidenceForDelivery(DEFAULT_MINIMUM_CONFIDENCE_FOR_DELIVERY)
                .annotationJobWithObjectsIdTruePositive(annotationJobWithObjectsIdTruePositive)
                .annotationJobWithObjectsIdFalsePositive(annotationJobWithObjectsIdFalsePositive)
                .annotationJobWithoutObjectsId(annotationJobWithoutObjectsId)
                .build()));
  }
}
