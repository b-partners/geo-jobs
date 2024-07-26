package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.HumanDetectedTileService;
import java.util.List;
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
    var annotationTaskId = submitted.getAnnotationTaskId();
    var imageSize = submitted.getImageSize();
    var xTile = submitted.getXTile();
    var yTile = submitted.getYTile();
    var zoom = submitted.getZoom();
    var annotationBatches = annotationService.getAnnotations(annotationJobId, annotationTaskId);
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
                      .detectedObjects(toHumanDetectedObject(tileId, annotations))
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

  private List<HumanDetectedObject> toHumanDetectedObject(
      String tileId, List<Annotation> annotations) {
    return annotations.stream()
        .map(
            ann ->
                HumanDetectedObject.builder()
                    .id(randomUUID().toString())
                    .label(ann.getLabel())
                    .confidence(ann.getComment())
                    .feature(ann.getPolygon())
                    .humanDetectedTileId(tileId)
                    .build())
        .toList();
  }
}
