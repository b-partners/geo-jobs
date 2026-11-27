package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationRetrievingTaskConsumerTest {
  private static final String ANNOTATION_JOB_ID = "annotationJobId";
  private static final String ANNOTATION_TASK_ID = "annotationTaskId";
  private static final String HUMAN_ZDJ_ID = "humanZdjId";

  AnnotationService annotationServiceMock = mock();
  HumanDetectedTileRepository humanDetectedTileRepositoryMock = mock();
  DetectionMapper detectionMapperMock = mock();
  AnnotationRetrievingTaskConsumer subject =
      new AnnotationRetrievingTaskConsumer(
          annotationServiceMock, humanDetectedTileRepositoryMock, detectionMapperMock);

  private AnnotationRetrievingTask task() {
    return AnnotationRetrievingTask.builder()
        .annotationJobId(ANNOTATION_JOB_ID)
        .annotationTaskId(ANNOTATION_TASK_ID)
        .humanZoneDetectionJobId(HUMAN_ZDJ_ID)
        .xTile(12)
        .yTile(34)
        .zoom(20)
        .build();
  }

  @SuppressWarnings("unchecked")
  private List<HumanDetectedTile> savedTiles() {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(humanDetectedTileRepositoryMock).saveAll(captor.capture());
    return (List<HumanDetectedTile>) captor.getValue();
  }

  @Test
  void accept_saves_one_human_detected_tile_per_annotation_batch() {
    var annotations = List.of(new Annotation());
    var detectedObjects = List.of(DetectedObject.builder().id("objectId").build());
    when(annotationServiceMock.getAnnotations(ANNOTATION_JOB_ID, ANNOTATION_TASK_ID))
        .thenReturn(List.of(new AnnotationBatch().annotations(annotations)));
    when(detectionMapperMock.toHumanDetectedObject(eq(20), anyString(), eq(annotations)))
        .thenReturn(detectedObjects);

    subject.accept(task());

    var tiles = savedTiles();
    assertEquals(1, tiles.size());
    var tile = tiles.getFirst();
    assertEquals(HUMAN_ZDJ_ID, tile.getJobId());
    assertEquals(ANNOTATION_JOB_ID, tile.getAnnotationJobId());
    assertEquals(ANNOTATION_TASK_ID, tile.getMachineDetectedTileId());
    assertEquals(1024, tile.getImageSize());
    assertEquals(detectedObjects, tile.getDetectedObjects());
    assertEquals(12, tile.getTile().getCoordinates().getX());
    assertEquals(34, tile.getTile().getCoordinates().getY());
    assertEquals(20, tile.getTile().getCoordinates().getZ());
    verify(detectionMapperMock).toHumanDetectedObject(20, tile.getId(), annotations);
  }

  @Test
  void accept_ignores_batches_without_annotations() {
    when(annotationServiceMock.getAnnotations(ANNOTATION_JOB_ID, ANNOTATION_TASK_ID))
        .thenReturn(
            List.of(
                new AnnotationBatch().annotations(null),
                new AnnotationBatch().annotations(List.of(new Annotation()))));
    when(detectionMapperMock.toHumanDetectedObject(anyInt(), anyString(), anyList()))
        .thenReturn(List.of());

    subject.accept(task());

    assertEquals(1, savedTiles().size());
  }

  @Test
  void accept_without_batches_saves_nothing() {
    when(annotationServiceMock.getAnnotations(ANNOTATION_JOB_ID, ANNOTATION_TASK_ID))
        .thenReturn(List.of());

    subject.accept(task());

    assertTrue(savedTiles().isEmpty());
  }

  @Test
  void with_new_status_appends_status_with_message() {
    var updated =
        AnnotationRetrievingTaskConsumer.withNewStatus(task(), FINISHED, FAILED, "failure message");

    assertEquals(FINISHED, updated.getStatus().getProgression());
    assertEquals(FAILED, updated.getStatus().getHealth());
    assertEquals("failure message", updated.getStatus().getMessage());
  }

  @Test
  void with_new_status_sets_processing_status_on_new_task() {
    var first =
        AnnotationRetrievingTaskConsumer.withNewStatus(task(), PROCESSING, UNKNOWN, "started");

    assertEquals(PROCESSING, first.getStatus().getProgression());
    assertEquals("started", first.getStatus().getMessage());
  }
}
