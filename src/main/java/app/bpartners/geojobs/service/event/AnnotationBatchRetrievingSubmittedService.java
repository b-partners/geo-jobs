package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AnnotationBatchRetrievingSubmittedService
    implements Consumer<AnnotationBatchRetrievingSubmitted> {
  private final AnnotationService annotationService;
  private final HumanDetectedTileService humanDetectedTileService;

  @Override
  public void accept(AnnotationBatchRetrievingSubmitted submitted) {
    var jobId = submitted.getJobId();
    var annotationJobId = submitted.getAnnotationJobId();
    var taskId = submitted.getTaskId();
    var imageSize = submitted.getImageSize();
    var xTile = submitted.getXTile();
    var yTile = submitted.getYTile();
    var zoom = submitted.getZoom();
    var annotationBatches = annotationService.getAnnotations(jobId, taskId);
    var humanDetectedTiles =
        annotationBatches.stream()
            .map(AnnotationBatch::getAnnotations)
            .filter(Objects::nonNull)
            .map(
                annotations -> {
                  var tileId = randomUUID().toString();
                  return HumanDetectedTile.builder()
                      .id(tileId)
                      .JobId(jobId)
                      .annotationJobId(annotationJobId)
                      .annotationTaskId(taskId)
                      .imageSize(imageSize)
                      .detectedObjects(
                          annotations.stream()
                              .map(
                                  ann ->
                                      HumanDetectedObject.builder()
                                          .label(ann.getLabel())
                                          .confidence(ann.getComment())
                                          .feature(
                                              new Feature()
                                                  .id(randomUUID().toString())
                                                  .zoom(zoom)
                                                  .geometry(null))
                                          .humanDetectedTileId(tileId)
                                          .build())
                              .toList())
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
  }
}
