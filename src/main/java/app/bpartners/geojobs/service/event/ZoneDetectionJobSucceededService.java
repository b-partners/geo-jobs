package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneDetectionJobSucceededService implements Consumer<ZoneDetectionJobSucceeded> {
  private final AnnotationService annotationService;
  private final DetectionTaskService detectionTaskService;
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final EventProducer eventProducer;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobSucceeded event) {
    log.warn("ZoneDetectionJobSucceeded {}, now handling human detection job", event);
    String humanZDJId = event.getHumanZdjId();
    String succeededJobId = event.getSucceededJobId();
    String humanDetectionJobId = randomUUID().toString();
    String annotationJobId = randomUUID().toString();
    List<DetectedTile> inDoubtTiles =
        detectionTaskService.findInDoubtTilesByJobId(succeededJobId).stream()
            .peek(detectedTile -> detectedTile.setHumanDetectionJobId(humanDetectionJobId))
            .toList();
    HumanDetectionJob savedHumanDetectionJob =
        humanDetectionJobRepository.save(
            HumanDetectionJob.builder()
                .id(humanDetectionJobId)
                .annotationJobId(annotationJobId)
                .inDoubtTiles(inDoubtTiles)
                .zoneDetectionJobId(humanZDJId)
                .build());
    log.warn(
        "HumanDetectionJob {} created, sending annotations to bpartners-annotation-api",
        savedHumanDetectionJob);
    try {
      annotationService.sendAnnotationsFromHumanZDJ(savedHumanDetectionJob);
    } catch (Exception e) {
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJob(savedHumanDetectionJob)
                  .attemptNb(1)
                  .build()));
    }
    log.warn(
        "HumanDetectionJob {} created, annotations sent to bpartners-annotation-api",
        savedHumanDetectionJob);
  }
}
