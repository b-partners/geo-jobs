package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.TO_REVIEW;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.api.AnnotatedJobsApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.AnnotatedTask;
import app.bpartners.gen.annotator.endpoint.rest.model.CrupdateAnnotatedJob;
import app.bpartners.gen.annotator.endpoint.rest.model.Job;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnnotationService {
  public static final int DEFAULT_IMAGES_HEIGHT = 1024;
  public static final int DEFAULT_IMAGES_WIDTH = 1024;
  private final AnnotatedJobsApi annotatedJobsApi;
  private final JobsApi jobsApi;
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
    this.jobsApi = new JobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.bucketComponent = bucketComponent;
  }

  public Job getAnnotationJobById(String annotationJobId) {
    try {
      return jobsApi.getJob(annotationJobId);
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }

  public void sendAnnotationsFromHumanZDJ(HumanDetectionJob humanDetectionJob)
      throws app.bpartners.gen.annotator.endpoint.rest.client.ApiException {
    String crupdateAnnotatedJobFolderPath = null;
    List<DetectedTile> inDoubtTiles = humanDetectionJob.getInDoubtTiles();
    String annotationJobId = humanDetectionJob.getAnnotationJobId();
    List<AnnotatedTask> annotatedTasks =
        taskExtractor.apply(inDoubtTiles, annotatorUserInfoGetter.getUserId());
    List<Label> extractLabelsFromTasks = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    List<Label> labels =
        extractLabelsFromTasks.isEmpty() // TODO: remove after debug
            ? List.of(
                new Label()
                    .id(randomUUID().toString())
                    .name(DetectableType.ROOF.name())
                    .color("#DFFF00"))
            : extractLabelsFromTasks;
    Instant now = Instant.now();
    log.error(
        "[DEBUG] AnnotationService : AnnotationJob(id={}) with labels (count={}, values={}) and"
            + " tasks (count={}, values={})",
        annotationJobId,
        labels.size(),
        labels,
        annotatedTasks.size(),
        annotatedTasks);
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
            .imagesHeight(DEFAULT_IMAGES_HEIGHT)
            .imagesWidth(DEFAULT_IMAGES_WIDTH)
            .teamId(annotatorUserInfoGetter.getTeamId()));
  }
}
