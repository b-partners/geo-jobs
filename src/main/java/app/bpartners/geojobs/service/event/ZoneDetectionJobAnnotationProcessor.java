package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreatedFailed;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneDetectionJobAnnotationProcessor {
  private final AnnotationService annotationService;
  private final DetectionTaskService detectionTaskService;
  private final DetectedTileRepository detectedTileRepository;
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final EventProducer<HumanDetectionJobCreatedFailed> eventProducer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final KeyPredicateFunction keyPredicateFunction;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final ExceptionToStringFunction exceptionToStringFunction;

  private List<DetectedTile> getInDoubtTiles(
      List<DetectedTile> detectedTiles,
      List<DetectableObjectConfiguration> detectableObjectConfigurations,
      String humanZDJFalsePositiveId) {
    return detectionTaskService
        .findInDoubtTilesByJobId(detectedTiles, detectableObjectConfigurations)
        .stream()
        .peek(detectedTile -> detectedTile.setHumanDetectionJobId(humanZDJFalsePositiveId))
        .toList();
  }

  private List<DetectedTile> getTilesWithoutObject(
      List<DetectedTile> detectedTiles, String inDoubtHumanDetectionJobId) {
    return detectedTiles.stream()
        .filter(detectedTile -> detectedTile.getDetectedObjects().isEmpty())
        .peek(detectedTile -> detectedTile.setHumanDetectionJobId(inDoubtHumanDetectionJobId))
        .toList();
  }

  private List<DetectedTile> getTruePositiveDetectedTiles(
      List<DetectedTile> inDoubtTiles, double minConfidence, String humanZDJTruePositiveId) {
    return inDoubtTiles.stream()
        .filter(
            detectedTile ->
                detectedTile.getDetectedObjects().stream()
                    .anyMatch(tile -> tile.getComputedConfidence() >= minConfidence))
        .peek(detectedTile -> detectedTile.setHumanDetectionJobId(humanZDJTruePositiveId))
        .toList();
  }

  private HumanDetectionJob save(
      String id, String jobId, List<DetectedTile> detectedTiles, String zoneId) {
    return humanDetectionJobRepository.save(
        HumanDetectionJob.builder()
            .id(id)
            .annotationJobId(jobId)
            .detectedTiles(detectedTiles)
            .zoneDetectionJobId(zoneId)
            .build());
  }

  @Transactional
  public AnnotationJobIds accept(
      String zoneDetectionJobId,
      Double minConfidence,
      String annotationJobWithObjectsIdTruePositive,
      String annotationJobWithObjectsIdFalsePositive,
      String annotationJobWithoutObjectsId) {
    String humanZDJTruePositiveId = randomUUID().toString();
    String humanZDJFalsePositiveId = randomUUID().toString();
    String inDoubtHumanDetectionJobId = randomUUID().toString();

    var humanJob = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJobId);
    var humanZDJId = humanJob.getId();

    List<DetectedTile> detectedTiles =
        detectedTileRepository.findAllByZdjJobId(zoneDetectionJobId).stream()
            .filter(keyPredicateFunction.apply(DetectedTile::getBucketPath))
            .toList();
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(zoneDetectionJobId);
    List<DetectedTile> inDoubtTiles =
        getInDoubtTiles(detectedTiles, detectableObjectConfigurations, humanZDJFalsePositiveId);
    List<DetectedTile> tilesWithoutObject =
        getTilesWithoutObject(detectedTiles, inDoubtHumanDetectionJobId);

    if (inDoubtTiles.isEmpty() && !tilesWithoutObject.isEmpty()) {
      log.error(
          "Any in doubt tiles detected from ZoneDetectionJob(id={})."
              + " {} tiles without detected objects are still sent, values are [{}]",
          zoneDetectionJobId,
          tilesWithoutObject.size(),
          tilesWithoutObject.stream().map(DetectedTile::describe).toList());
    }
    var truePositiveDetectedTiles =
        getTruePositiveDetectedTiles(inDoubtTiles, minConfidence, humanZDJTruePositiveId);

    var falsePositiveTiles = new ArrayList<>(inDoubtTiles);
    falsePositiveTiles.removeAll(truePositiveDetectedTiles);

    HumanDetectionJob savedHumanZDJTruePositive =
        save(
            humanZDJTruePositiveId,
            annotationJobWithObjectsIdTruePositive,
            truePositiveDetectedTiles,
            humanZDJId);
    HumanDetectionJob savedHumanZDJFalsePositive =
        save(
            humanZDJFalsePositiveId,
            annotationJobWithObjectsIdFalsePositive,
            falsePositiveTiles,
            humanZDJId);
    HumanDetectionJob savedHumanDetectionJobWithoutTile =
        save(inDoubtHumanDetectionJobId, annotationJobWithoutObjectsId, inDoubtTiles, humanZDJId);

    computeTilesIntoAnnotatedJobs(
        truePositiveDetectedTiles.isEmpty(),
        savedHumanZDJTruePositive,
        humanJob.getZoneName()
            + " - "
            + truePositiveDetectedTiles.size()
            + " tiles with detection confidence >= "
            + minConfidence * 100
            + "%"
            + " "
            + now(),
        "No potential true positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");

    computeTilesIntoAnnotatedJobs(
        falsePositiveTiles.isEmpty(),
        savedHumanZDJFalsePositive,
        humanJob.getZoneName()
            + " - "
            + falsePositiveTiles.size()
            + " tiles with detection confidence < "
            + minConfidence * 100
            + "%"
            + " "
            + now(),
        "No potential false positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");

    computeTilesIntoAnnotatedJobs(
        tilesWithoutObject.isEmpty(),
        savedHumanDetectionJobWithoutTile,
        humanJob.getZoneName()
            + " - "
            + tilesWithoutObject.size()
            + " tiles without detected objects"
            + " "
            + now(),
        "No tiles without objects found from ZDJ(id=" + zoneDetectionJobId + ")");

    return new AnnotationJobIds(
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }

  private void computeTilesIntoAnnotatedJobs(
      boolean detectedTilesIsEmpty,
      HumanDetectionJob humanDetectionJob,
      String annotationJobName,
      String emptyTilesMessage) {
    try {
      if (!detectedTilesIsEmpty) {
        annotationService.createAnnotationJob(humanDetectionJob, annotationJobName);
      } else {
        log.error(emptyTilesMessage);
      }
    } catch (Exception e) {
      log.error("Exception occurred when creating annotationJob {}", e.getMessage());
      eventProducer.accept(
          List.of(
              HumanDetectionJobCreatedFailed.builder()
                  .humanDetectionJobId(humanDetectionJob.getId())
                  .annotationJobCustomName(annotationJobName)
                  .exceptionMessage(exceptionToStringFunction.apply(e))
                  .attemptNb(1)
                  .build()));
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @AllArgsConstructor
  @Data
  public static class AnnotationJobIds {
    private String jobWithDetectedTruePositiveId;
    private String jobWithDetectedFalsePositiveId;
    private String jobWithoutDetectedObjectsId;
  }
}
