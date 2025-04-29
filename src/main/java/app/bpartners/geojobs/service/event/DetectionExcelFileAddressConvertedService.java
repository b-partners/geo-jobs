package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionExcelFileAddressConverted;
import app.bpartners.geojobs.endpoint.event.model.zone.DetectionAddressConversionJobCreated;
import app.bpartners.geojobs.service.DetectionAddressConversionJobMapper;
import app.bpartners.geojobs.service.DetectionAddressConversionJobService;
import app.bpartners.geojobs.service.DetectionAddressConversionTaskMapper;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionExcelFileAddressConvertedService
    implements Consumer<DetectionExcelFileAddressConverted> {
  private final DetectionAddressConversionJobMapper detectionAddressConversionJobMapper;
  private final DetectionAddressConversionJobService detectionAddressConversionJobService;
  private final EventProducer eventProducer;
  private final DetectionAddressConversionTaskMapper detectionAddressConversionTaskMapper;

  @Override
  public void accept(DetectionExcelFileAddressConverted event) {
    var detection = event.getDetection();
    var detectionAddressConversionJob =
        detectionAddressConversionJobMapper.fromDetection(detection);
    var detectionAddressConversionTasks =
        detectionAddressConversionTaskMapper.fromAddressListAndJobId(
            detection, detectionAddressConversionJob.getId());

    var savedAddressConversionJob =
        detectionAddressConversionJobService.create(
            detectionAddressConversionJob, detectionAddressConversionTasks);

    eventProducer.accept(
        List.of(
            DetectionAddressConversionJobCreated.builder().job(savedAddressConversionJob).build()));
  }
}
