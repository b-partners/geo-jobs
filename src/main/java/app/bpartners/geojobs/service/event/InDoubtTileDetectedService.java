package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.annotator.gen.JobStatus.PENDING;
import static app.bpartners.geojobs.service.annotator.TaskExtractor.formatToAnnotatorFilePath;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.gen.InDoubtTilesDetected;
import app.bpartners.geojobs.file.annotator.AnnotatorBucketComponent;
import app.bpartners.geojobs.file.self.BucketComponent;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.annotator.AnnotatorApiClient;
import app.bpartners.geojobs.repository.annotator.gen.AnnotatedTask;
import app.bpartners.geojobs.repository.annotator.gen.CrupdateAnnotatedJob;
import app.bpartners.geojobs.repository.annotator.gen.Label;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotatorUserInfoGetter;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
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
  private final TaskExtractor taskExtractor;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent selfBucket;
  private final AnnotatorBucketComponent annotatorBucketComponent;

  @Override
  public void accept(InDoubtTilesDetected event) {
    String jobId = event.getJobId();
    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(jobId);
    List<DetectedTile> detectedInDoubtTiles =
        detectedTiles.stream()
            .filter(
                detectedTile ->
                    detectedTile.getDetectedObjects().stream().anyMatch(DetectedObject::isInDoubt))
            .toList();
    String crupdateAnnotatedJobId = randomUUID().toString();
    String crupdateAnnotatedJobFolderPath = crupdateAnnotatedJobId + "/";
    detectedInDoubtTiles.forEach(
        tile -> {
          selfBucket.copyTo(
              tile.getBucketPath(),
              annotatorBucketComponent.getBucketName(),
              crupdateAnnotatedJobFolderPath + formatToAnnotatorFilePath(tile.getBucketPath()));
        });
    List<AnnotatedTask> annotatedTasks =
        taskExtractor.apply(detectedInDoubtTiles, annotatorUserInfoGetter.getUserId());
    List<Label> labels = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    Instant now = Instant.now();
    annotatorApiClient.crupdateAnnotatedJob(
        CrupdateAnnotatedJob.builder()
            .id(crupdateAnnotatedJobId)
            .name("geo-jobs" + now)
            .bucketName(annotatorBucketComponent.getBucketName())
            .folderPath(crupdateAnnotatedJobFolderPath)
            .labels(labels)
            .ownerEmail("tech@bpartners.app")
            .status(PENDING)
            .annotatedTasks(annotatedTasks)
            .teamId(annotatorUserInfoGetter.getTeamId())
            .build());
  }
}
