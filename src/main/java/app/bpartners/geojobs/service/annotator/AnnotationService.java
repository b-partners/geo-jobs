package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.*;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.CreateAnnotatedTaskExtracted;
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
  private final JobsApi jobsApi;
  private final TaskExtractor taskExtractor;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final AdminApi adminApi;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      BucketComponent bucketComponent,
      EventProducer eventProducer) {
    this.jobsApi = new JobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.adminApi = new AdminApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.bucketComponent = bucketComponent;
    this.eventProducer = eventProducer;
  }

  public Job getAnnotationJobById(String annotationJobId) {
    try {
      return jobsApi.getJob(annotationJobId);
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }

  public void createAnnotationJob(HumanDetectionJob humanDetectionJob)
      throws app.bpartners.gen.annotator.endpoint.rest.client.ApiException {
    String crupdateAnnotatedJobFolderPath = null;
    List<DetectedTile> inDoubtTiles = humanDetectionJob.getInDoubtTiles();
    log.error(
        "[DEBUG] AnnotationService InDoubtTiles [size={}, tiles={}]",
        inDoubtTiles.size(),
        inDoubtTiles.stream().map(DetectedTile::describe).toList());
    String annotationJobId = humanDetectionJob.getAnnotationJobId();
    List<CreateAnnotatedTask> annotatedTasks =
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
            + " tasks (count={})",
        annotationJobId,
        labels.size(),
        labels,
        annotatedTasks.size(),
        annotatedTasks);
    Job createdAnnotationJob =
        jobsApi.saveJob(
            annotationJobId,
            new CrupdateJob()
                .id(annotationJobId)
                .name("geo-jobs" + now)
                .bucketName(bucketComponent.getBucketName())
                .folderPath(crupdateAnnotatedJobFolderPath)
                .labels(labels)
                .ownerEmail("tech@bpartners.app")
                .status(PENDING)
                .type(REVIEWING)
                .imagesHeight(DEFAULT_IMAGES_HEIGHT)
                .imagesWidth(DEFAULT_IMAGES_WIDTH)
                .teamId(annotatorUserInfoGetter.getTeamId()));

    annotatedTasks.forEach(
        task -> {
          eventProducer.accept(
              List.of(new CreateAnnotatedTaskExtracted(createdAnnotationJob.getId(), task)));
        });
  }

  public void addAnnotationTask(String jobId, CreateAnnotatedTask annotatedTask) {
    try {
      adminApi.addAnnotatedTasksToAnnotatedJob(jobId, List.of(annotatedTask));
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }
}
