package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionAddressConversionJobCreated;
import app.bpartners.geojobs.service.DetectionAddressConversionJobService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionAddressConversionJobCreatedService
    implements Consumer<DetectionAddressConversionJobCreated> {
  private final DetectionAddressConversionJobService detectionAddressConversionJobService;
  private final EventProducer eventProducer;

  @Override
  public void accept(DetectionAddressConversionJobCreated event) {
    var detectionAddressConversionJob = event.getJob();

    detectionAddressConversionJobService.fireTasks(detectionAddressConversionJob.getId());

    eventProducer.accept(
        List.of(
            new DetectionAddressConversionJobStatusRecomputingSubmitted(
                detectionAddressConversionJob.getId())));
  }
}
