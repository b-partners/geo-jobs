package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionVGGUpdate;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionVGGRequestedService implements Consumer<DetectionVGGRequested> {
  private final DetectionRepository detectionRepository;
  private final VGGFactory vggFactory;
  private final DetectionVGGUpdate detectionVGGUpdate;

  @Override
  public void accept(DetectionVGGRequested event) {
    var detectionId = event.getDetectionId();
    var detection = detectionRepository.findById(detectionId).orElseThrow();
    var filteredTiledPixelPolygons = event.getFilteredTiledPixelPolygons();
    var vgg = vggFactory.from(filteredTiledPixelPolygons);

    var newDetection = detectionVGGUpdate.apply(vgg, detection);

    detectionRepository.save(newDetection);
  }
}
