package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.*;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.LocalTime.now;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.*;
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
  private final LabelConverter labelConverter;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent bucketComponent;
  private final EventProducer<CreateAnnotatedTaskSubmitted> eventProducer;
  private final AdminApi adminApi;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelConverter labelConverter,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      BucketComponent bucketComponent,
      EventProducer<CreateAnnotatedTaskSubmitted> eventProducer) {
    this.jobsApi = new JobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.adminApi = new AdminApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelConverter = labelConverter;
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

  public void createAnnotationJob(HumanDetectionJob humanDetectionJob, String jobName)
      throws ApiException {
    String folderPath = null;
    List<DetectedTile> detectedTiles = humanDetectionJob.getDetectedTiles();
    log.info(
        "[DEBUG] AnnotationService detected tiles [size={}, tiles={}]",
        detectedTiles.size(),
        detectedTiles.stream().map(DetectedTile::describe).toList());
    String annotationJobId = humanDetectionJob.getAnnotationJobId();
    List<DetectableObjectConfiguration> detectableObjects =
        humanDetectionJob.getDetectableObjectConfigurations();
    List<Label> expectedLabels =
        detectableObjects.stream()
            .map(object -> labelConverter.apply(object.getObjectType()))
            .toList();
    List<CreateAnnotatedTask> annotatedTasks =
        taskExtractor.apply(detectedTiles, annotatorUserInfoGetter.getUserId(), expectedLabels);
    List<Label> extractLabelsFromTasks = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    List<Label> labels = extractLabelsFromTasks.isEmpty() ? expectedLabels : extractLabelsFromTasks;
    log.error(
        "[DEBUG] AnnotationService : AnnotationJob(id={}) with labels (count={}, values={}) and"
            + " tasks (count={})",
        annotationJobId,
        labels.size(),
        labels,
        annotatedTasks.size());
    Job createdAnnotationJob =
        jobsApi.saveJob(
            annotationJobId,
            new CrupdateJob()
                .id(annotationJobId)
                .name(jobName)
                .bucketName(bucketComponent.getBucketName())
                .folderPath(folderPath)
                .labels(labels)
                .ownerEmail("tech@bpartners.app")
                .status(PENDING)
                .type(REVIEWING)
                .imagesHeight(DEFAULT_IMAGES_HEIGHT)
                .imagesWidth(DEFAULT_IMAGES_WIDTH)
                .teamId(annotatorUserInfoGetter.getTeamId()));

    annotatedTasks.forEach(
        task ->
            eventProducer.accept(
                List.of(new CreateAnnotatedTaskSubmitted(createdAnnotationJob.getId(), task))));
  }

  public void createAnnotationJob(HumanDetectionJob humanDetectionJob) throws ApiException {
    createAnnotationJob(humanDetectionJob, "geo-jobs" + now());
  }

  public void addAnnotationTask(String jobId, CreateAnnotatedTask annotatedTask) {
    try {
      adminApi.addAnnotatedTasksToAnnotatedJob(jobId, List.of(annotatedTask));
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(SERVER_EXCEPTION, e);
    }
  }
}
