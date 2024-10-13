package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreated;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.HumanDetectionJobService;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionTaskService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import lombok.*;
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
  private final HumanDetectionJobService humanDetectionJobService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final KeyPredicateFunction keyPredicateFunction;
  private final EventProducer eventProducer;

  @Transactional
  public AnnotationJobIds accept(
      String zoneDetectionJobId,
      Double minConfidence,
      String annotationJobWithObjectsIdTruePositive,
      String annotationJobWithObjectsIdFalsePositive,
      String annotationJobWithoutObjectsId,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    ZoneDetectionJob humanJob = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJobId);
    List<MachineDetectedTile> machineDetectedTiles =
        detectedTileRepository.findAllByZdjJobId(zoneDetectionJobId).stream()
            .filter(keyPredicateFunction.apply(MachineDetectedTile::getBucketPath))
            .toList();

    processFilterWithoutDetectedObjects(
        zoneDetectionJobId,
        annotationJobWithoutObjectsId,
        machineDetectedTiles,
        humanJob,
        detectableObjectConfigurations);

    processFilterTruePositive(
        zoneDetectionJobId,
        minConfidence,
        annotationJobWithObjectsIdTruePositive,
        machineDetectedTiles,
        humanJob,
        detectableObjectConfigurations);

    processFilterFalsePositive(
        zoneDetectionJobId,
        minConfidence,
        annotationJobWithObjectsIdFalsePositive,
        machineDetectedTiles,
        humanJob,
        detectableObjectConfigurations);

    return new AnnotationJobIds(
        annotationJobWithObjectsIdTruePositive,
        annotationJobWithObjectsIdFalsePositive,
        annotationJobWithoutObjectsId);
  }

  private void processFilterFalsePositive(
      String zoneDetectionJobId,
      Double minConfidence,
      String annotationJobId,
      List<MachineDetectedTile> machineDetectedTiles,
      ZoneDetectionJob humanZDJ,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    var humanDetectionJobId = randomUUID().toString();
    var isGreaterThan = false;
    List<MachineDetectedTile> inDoubtTiles =
        detectionTaskService.filterByInDoubt(machineDetectedTiles, detectableObjectConfigurations);
    var falsePositiveTiles =
        detectionTaskService.filterByConfidence(minConfidence, inDoubtTiles, isGreaterThan).stream()
            .map(
                detectedTile ->
                    detectedTile.toBuilder().humanDetectionJobId(humanDetectionJobId).build())
            .toList();
    var hdjFalsePositiveDetectedObjects =
        humanDetectionJobService.create(
            annotationJobId,
            humanDetectionJobId,
            falsePositiveTiles,
            humanZDJ.getId(),
            detectableObjectConfigurations);
    if (falsePositiveTiles.isEmpty()) {
      log.warn("No potential false positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");
    } else {
      var jobName =
          humanZDJ.getZoneName()
              + " - "
              + falsePositiveTiles.size()
              + " tiles with detection confidence < "
              + minConfidence * 100
              + "%"
              + " "
              + now();

      eventProducer.accept(
          List.of(new HumanDetectionJobCreated(hdjFalsePositiveDetectedObjects.getId(), jobName)));
    }
  }

  private void processFilterTruePositive(
      String zoneDetectionJobId,
      Double minConfidence,
      String annotationJobId,
      List<MachineDetectedTile> machineDetectedTiles,
      ZoneDetectionJob humanZDJ,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    var humanDetectionJobId = randomUUID().toString();
    var isGreaterThan = true;
    var inDoubtTiles =
        detectionTaskService.filterByInDoubt(machineDetectedTiles, detectableObjectConfigurations);
    var truePositiveDetectedTiles =
        detectionTaskService.filterByConfidence(minConfidence, inDoubtTiles, isGreaterThan).stream()
            .map(
                detectedTile ->
                    detectedTile.toBuilder().humanDetectionJobId(humanDetectionJobId).build())
            .toList();
    var hdjTruePositiveDetectedObjects =
        humanDetectionJobService.create(
            annotationJobId,
            humanDetectionJobId,
            truePositiveDetectedTiles,
            humanZDJ.getId(),
            detectableObjectConfigurations);
    if (truePositiveDetectedTiles.isEmpty()) {
      log.warn("No potential true positive objects found from ZDJ(id=" + zoneDetectionJobId + ")");
    } else {
      var jobName =
          humanZDJ.getZoneName()
              + " - "
              + truePositiveDetectedTiles.size()
              + " tiles with detection confidence >= "
              + minConfidence * 100
              + "%"
              + " "
              + now();

      eventProducer.accept(
          List.of(new HumanDetectionJobCreated(hdjTruePositiveDetectedObjects.getId(), jobName)));
    }
  }

  private void processFilterWithoutDetectedObjects(
      String zoneDetectionJobId,
      String annotationJobId,
      List<MachineDetectedTile> machineDetectedTiles,
      ZoneDetectionJob humanZDJ,
      List<DetectableObjectConfiguration> detectableObjectConfigurations) {
    String humanDetectionJobId = randomUUID().toString();
    List<MachineDetectedTile> tilesWithoutObject =
        machineDetectedTiles.stream()
            .filter(detectedTile -> detectedTile.getDetectedObjects().isEmpty())
            .map(
                detectedTile ->
                    detectedTile.toBuilder().humanDetectionJobId(humanDetectionJobId).build())
            .toList();
    HumanDetectionJob hdjWithoutDetectedObjects =
        humanDetectionJobService.create(
            annotationJobId,
            humanDetectionJobId,
            tilesWithoutObject,
            humanZDJ.getId(),
            detectableObjectConfigurations);
    if (tilesWithoutObject.isEmpty()) {
      log.warn("No tiles without objects found from ZDJ(id=" + zoneDetectionJobId + ")");
    } else {
      var jobName =
          humanZDJ.getZoneName()
              + " - "
              + tilesWithoutObject.size()
              + " tiles without detected objects"
              + " "
              + now();

      eventProducer.accept(
          List.of(new HumanDetectionJobCreated(hdjWithoutDetectedObjects.getId(), jobName)));
    }
  }

  @AllArgsConstructor
  @Data
  @Builder
  public static class AnnotationJobIds {
    private String jobWithDetectedTruePositiveId;
    private String jobWithDetectedFalsePositiveId;
    private String jobWithoutDetectedObjectsId;
  }
}
