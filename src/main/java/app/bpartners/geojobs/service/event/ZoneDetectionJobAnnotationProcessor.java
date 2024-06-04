package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneDetectionJobAnnotationProcessor {
  public static final double MIN_CONFIDENCE_TRUE_POSITIVE = 0.8;
  private final AnnotationService annotationService;
  private final DetectionTaskService detectionTaskService;
  private final DetectedTileRepository detectedTileRepository;
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final EventProducer eventProducer;
  private final ZoneDetectionJobService zoneDetectionJobService;

  public AnnotationJobIds accept(
      String zoneDetectionJobId,
      String annotationJobWithObjectsIdTruePositive,
      String annotationJobWithObjectsIdFalsePositive,
      String annotationJobWithoutObjectsId) {
    String humanZDJTruePositiveId = randomUUID().toString();
    String humanZDJFalsePositiveId = randomUUID().toString();
    String inDoubtHumanDetectionJobId = randomUUID().toString();

    var humanJob = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJobId);
    var humanZDJId = humanJob.getId();

    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(zoneDetectionJobId);
    List<DetectedTile> inDoubtTiles =
        detectionTaskService.findInDoubtTilesByJobId(zoneDetectionJobId, detectedTiles).stream()
            .peek(detectedTile -> detectedTile.setHumanDetectionJobId(humanZDJFalsePositiveId))
            .toList();
    List<DetectedTile> tilesWithoutObject =
        detectedTiles.stream()
            .filter(detectedTile -> detectedTile.getDetectedObjects().isEmpty())
            .peek(detectedTile -> detectedTile.setHumanDetectionJobId(inDoubtHumanDetectionJobId))
            .toList();
    if (inDoubtTiles.isEmpty() && !tilesWithoutObject.isEmpty()) {
      log.error(
          "Any in doubt tiles detected from ZoneDetectionJob(id={})."
              + " {} tiles without detected objects are still sent, values are [{}]",
          zoneDetectionJobId,
          tilesWithoutObject.size(),
          tilesWithoutObject.stream().map(DetectedTile::describe).toList());
    }
    var truePositiveDetectedTiles =
        inDoubtTiles.stream()
            .filter(
                detectedTile ->
                    detectedTile.getDetectedObjects().stream()
                        .anyMatch(
                            tile -> tile.getComputedConfidence() >= MIN_CONFIDENCE_TRUE_POSITIVE))
            .peek(detectedTile -> detectedTile.setHumanDetectionJobId(humanZDJTruePositiveId))
            .toList();
    var falsePositiveTiles = new ArrayList<>(inDoubtTiles);
    falsePositiveTiles.removeAll(truePositiveDetectedTiles);
    HumanDetectionJob savedHumanZDJTruePositive =
        humanDetectionJobRepository.save(
            HumanDetectionJob.builder()
                .id(humanZDJTruePositiveId)
                .annotationJobId(annotationJobWithObjectsIdTruePositive)
                .detectedTiles(truePositiveDetectedTiles)
                .zoneDetectionJobId(humanZDJId)
                .build());
    HumanDetectionJob savedHumanZDJFalsePositive =
        humanDetectionJobRepository.save(
            HumanDetectionJob.builder()
                .id(humanZDJFalsePositiveId)
                .annotationJobId(annotationJobWithObjectsIdFalsePositive)
                .detectedTiles(falsePositiveTiles)
                .zoneDetectionJobId(humanZDJId)
                .build());
    HumanDetectionJob savedHumanDetectionJobWithoutTile =
        humanDetectionJobRepository.save(
            HumanDetectionJob.builder()
                .id(inDoubtHumanDetectionJobId)
                .annotationJobId(annotationJobWithoutObjectsId)
                .detectedTiles(tilesWithoutObject)
                .zoneDetectionJobId(humanZDJId)
                .build());

    savedHumanZDJTruePositive.setDetectedTiles(
        truePositiveDetectedTiles); // TODO: check if still necessary
    savedHumanZDJFalsePositive.setDetectedTiles(
        falsePositiveTiles); // TODO: check if still necessary
    savedHumanDetectionJobWithoutTile.setDetectedTiles(
        tilesWithoutObject); // TODO: check if still necessary

    detectedTileRepository.saveAll(
        Stream.of(truePositiveDetectedTiles, falsePositiveTiles, tilesWithoutObject)
            .flatMap(List::stream)
            .toList()); // TODO: check if still necessary
    try {
      if (!truePositiveDetectedTiles.isEmpty()) {
        annotationService.createAnnotationJob(
            savedHumanZDJTruePositive,
            humanJob.getZoneName()
                + " - "
                + truePositiveDetectedTiles.size()
                + " tiles with confidence >= 95%"
                + " "
                + now());
      } else {
        log.error(
            "No potential true positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");
      }
    } catch (Exception e) {
      log.error("Exception occurred when creating annotationJob {}", e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJobId(savedHumanZDJFalsePositive.getId())
                  .attemptNb(1)
                  .build()));
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    try {
      if (!falsePositiveTiles.isEmpty()) {
        annotationService.createAnnotationJob(
            savedHumanZDJFalsePositive,
            humanJob.getZoneName()
                + " - "
                + falsePositiveTiles.size()
                + " tiles with confidence < 95%"
                + " "
                + now());
      } else {
        log.error(
            "No potential false positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");
      }
    } catch (Exception e) {
      log.error("Exception occurred when creating annotationJob {}", e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJobId(savedHumanZDJFalsePositive.getId())
                  .attemptNb(1)
                  .build()));
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    try {
      if (!tilesWithoutObject.isEmpty()) {
        annotationService.createAnnotationJob(
            savedHumanDetectionJobWithoutTile,
            humanJob.getZoneName()
                + " - "
                + tilesWithoutObject.size()
                + " tiles without objects"
                + " "
                + now());
      } else {
        log.error("No tiles without objects found from ZDJ(id=" + zoneDetectionJobId + ")");
      }
    } catch (Exception e) {
      log.error("Exception occurred when creating annotationJob {}", e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJobId(savedHumanDetectionJobWithoutTile.getId())
                  .attemptNb(1)
                  .build()));
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "HumanDetectionJob {} created, annotations sent to bpartners-annotation-api",
        savedHumanZDJFalsePositive);
    return new AnnotationJobIds(
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }

  @AllArgsConstructor
  @Data
  public static class AnnotationJobIds {
    private String jobWithDetectedTruePositiveId;
    private String jobWithDetectedFalsePositiveId;
    private String jobWithoutDetectedObjectsId;
  }
}
