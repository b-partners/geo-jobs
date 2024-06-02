package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.ZoneDetectionJobSucceeded;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneDetectionJobSucceededService implements Consumer<ZoneDetectionJobSucceeded> {
  private final ZoneDetectionJobAnnotationProcessor jobAnnotationProcessor;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    log.info("ZoneDetectionJobSucceeded {}, now handling human detection job", event);
    String succeededJobId = event.getSucceededJobId();
    jobAnnotationProcessor.accept(succeededJobId);
  }
}
