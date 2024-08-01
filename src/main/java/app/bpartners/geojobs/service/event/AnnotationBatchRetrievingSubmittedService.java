package app.bpartners.geojobs.service.event;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.AnnotationRetrievingJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.AnnotationRetrievingTaskService;
import app.bpartners.geojobs.service.annotator.AnnotationRetrievingTaskStatusService;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class AnnotationBatchRetrievingSubmittedService
    implements Consumer<AnnotationBatchRetrievingSubmitted> {
  private final AnnotationService annotationService;
  private final HumanDetectedTileService humanDetectedTileService;
  private final DetectionMapper detectionMapper;
  private final AnnotationRetrievingTaskService annotationRetrievingTaskService;
  private final AnnotationRetrievingTaskStatusService annotationRetrievingTaskStatusService;
  private final EventProducer<AnnotationRetrievingJobStatusRecomputingSubmitted> eventProducer;

  @Override
  public void accept(AnnotationBatchRetrievingSubmitted submitted) {
    var jobId = submitted.getJobId();
    var annotationJobId = submitted.getAnnotationJobId();
    var annotationTaskId = submitted.getAnnotationTaskId();
    var imageSize = submitted.getImageSize();
    var xTile = submitted.getXTile();
    var yTile = submitted.getYTile();
    var zoom = submitted.getZoom();
    var retrievingTask = annotationRetrievingTaskService.getByAnnotationTaskId(annotationTaskId);
    var processed = annotationRetrievingTaskStatusService.process(retrievingTask);
    var annotationBatches = annotationService.getAnnotations(annotationJobId, annotationTaskId);
    try {
      var humanDetectedTiles =
          annotationBatches.stream()
              .map(AnnotationBatch::getAnnotations)
              .filter(Objects::nonNull)
              .map(
                  annotations -> {
                    var tileId = randomUUID().toString();
                    return HumanDetectedTile.builder()
                        .id(tileId)
                        .jobId(jobId)
                        .annotationJobId(annotationJobId)
                        .machineDetectedTileId(annotationTaskId)
                        .imageSize(imageSize)
                        .detectedObjects(
                            detectionMapper.toHumanDetectedObject(zoom, tileId, annotations))
                        .tile(
                            Tile.builder()
                                .id(randomUUID().toString())
                                .creationDatetime(now())
                                .coordinates(new TileCoordinates().x(xTile).y(yTile).z(zoom))
                                .build())
                        .build();
                  })
              .toList();
      humanDetectedTileService.saveAll(humanDetectedTiles);
      var succeededTask = annotationRetrievingTaskStatusService.succeed(processed);
      eventProducer.accept(List.of(new AnnotationRetrievingJobStatusRecomputingSubmitted(succeededTask.getJobId())));
    } catch (RuntimeException e) {
      log.error("Error when creating human detected tile {}", e.getMessage());
      annotationRetrievingTaskStatusService.fail(processed);
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }
}
