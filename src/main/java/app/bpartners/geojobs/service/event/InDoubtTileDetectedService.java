package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.gen.InDoubtTilesDetected;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.annotator.AnnotatorApiClient;
import app.bpartners.geojobs.repository.annotator.gen.AnnotatedTask;
import app.bpartners.geojobs.repository.annotator.gen.Annotation;
import app.bpartners.geojobs.repository.annotator.gen.AnnotationBatch;
import app.bpartners.geojobs.repository.annotator.gen.CrupdateAnnotatedJob;
import app.bpartners.geojobs.repository.annotator.gen.JobStatus;
import app.bpartners.geojobs.repository.annotator.gen.Label;
import app.bpartners.geojobs.repository.annotator.gen.Point;
import app.bpartners.geojobs.repository.annotator.gen.Polygon;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InDoubtTileDetectedService implements Consumer<InDoubtTilesDetected> {
  private final DetectedTileRepository detectedTileRepository;
  private final AnnotatorApiClient annotatorApiClient;

  @Override
  public void accept(InDoubtTilesDetected event) {
    String jobId = event.getJobId();
    Instant updatedAt = Instant.now();
    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(jobId);
    List<DetectedTile> detectedInDoubtTiles =
        detectedTiles.stream()
            .filter(
                detectedTile ->
                    detectedTile.getDetectedObjects().stream().anyMatch(DetectedObject::isInDoubt))
            .toList();
    // /!\ TODO: complete TODO by converting detected in-doubt tiles

    Label label = Label.builder().id(randomUUID().toString()).name("TODO").color("TODO").build();

    annotatorApiClient.crupdateAnnotatedJob(
        CrupdateAnnotatedJob.builder()
            .id(randomUUID().toString())
            .name("TODO")
            .bucketName("TODO")
            .folderPath("TODO")
            .labels(List.of(label))
            .ownerEmail(null)
            .status(JobStatus.TO_REVIEW)
            .annotatedTasks(
                List.of(
                    AnnotatedTask.builder()
                        .id(randomUUID().toString())
                        .annotatorId("TODO")
                        .filename("TODO")
                        .annotationBatch(
                            AnnotationBatch.builder()
                                .id(randomUUID().toString())
                                .creationDatetime(updatedAt)
                                .annotations(
                                    List.of(
                                        Annotation.builder()
                                            .id(randomUUID().toString())
                                            .userId("TODO")
                                            .taskId("TODO")
                                            .label(label)
                                            .polygon(
                                                Polygon.builder()
                                                    .points(
                                                        List.of(
                                                            Point.builder()
                                                                .y(0.0) // TODO
                                                                .y(0.0) // TODO
                                                                .build()))
                                                    .build())
                                            .build()))
                                .build())
                        .build()))
            .teamId("TODO")
            .build());
  }
}
