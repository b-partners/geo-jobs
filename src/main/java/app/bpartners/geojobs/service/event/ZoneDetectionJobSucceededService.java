package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

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
  public static final double DEFAULT_MIN_CONFIDENCE = 0.8;
  private final ZoneDetectionJobAnnotationProcessor jobAnnotationProcessor;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    log.info("ZoneDetectionJobSucceeded {}, now handling human detection job", event);
    String succeededJobId = event.getSucceededJobId();
    var annotationJobWithObjectsIdTruePositive = randomUUID().toString();
    var annotationJobWithObjectsIdFalsePositive = randomUUID().toString();
    var annotationJobWithoutObjectsId = randomUUID().toString();
    jobAnnotationProcessor.accept(
        succeededJobId,
        DEFAULT_MIN_CONFIDENCE,
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }
}
