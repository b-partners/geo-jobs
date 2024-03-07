package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.TO_REVIEW;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;

import app.bpartners.gen.annotator.endpoint.rest.api.AnnotatedJobsApi;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.CrupdateAnnotatedJob;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnnotationService {
  private final AnnotatedJobsApi annotatedJobsApi;
  private final TaskExtractor taskExtractor;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent bucketComponent;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      BucketComponent bucketComponent) {
    this.annotatedJobsApi = new AnnotatedJobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.bucketComponent = bucketComponent;
  }

  public void sendAnnotationsFromHumanZDJ(HumanDetectionJob humanDetectionJob)
      throws app.bpartners.gen.annotator.endpoint.rest.client.ApiException {
    String crupdateAnnotatedJobFolderPath = "/"; // TODO: can this be null ?
    List<DetectedTile> inDoubtTiles = humanDetectionJob.getInDoubtTiles();
    String annotationJobId = humanDetectionJob.getAnnotationJobId();
    List<AnnotatedTask> annotatedTasks =
        taskExtractor.apply(inDoubtTiles, annotatorUserInfoGetter.getUserId());
    List<Label> labels = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    Instant now = Instant.now();

    annotatedJobsApi.crupdateAnnotatedJob(
        annotationJobId,
        new CrupdateAnnotatedJob()
            .id(annotationJobId)
            .name("geo-jobs" + now)
            .bucketName(bucketComponent.getBucketName())
            .folderPath(crupdateAnnotatedJobFolderPath)
            .labels(labels)
            .ownerEmail("tech@bpartners.app")
            .status(TO_REVIEW)
            .type(REVIEWING)
            .annotatedTasks(annotatedTasks)
            .teamId(annotatorUserInfoGetter.getTeamId()));
  }
}
