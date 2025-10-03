package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionTilingRequested;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionAreaValidator;
import app.bpartners.geojobs.service.detection.DetectionTilingCreation;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionTilingRequestedService implements Consumer<DetectionTilingRequested> {
  private final DetectionRepository detectionRepository;
  private final DetectionTilingCreation detectionTilingCreation;
  private final DetectionAreaValidator detectionAreaValidator;

  @Override
  public void accept(DetectionTilingRequested detectionTilingRequested) {
    var detectionIdentifier = detectionTilingRequested.getDetectionIdentifier();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();

    var detectionWithTilingCreated = detectionTilingCreation.apply(detection);

    detectionAreaValidator.accept(detectionWithTilingCreated);
  }
}
