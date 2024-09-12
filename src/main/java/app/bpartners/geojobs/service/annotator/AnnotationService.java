package app.bpartners.geojobs.service.annotator;

import static app.bpartners.gen.annotator.endpoint.rest.model.JobStatus.*;
import static app.bpartners.gen.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.LocalTime.now;
import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.api.AdminApi;
import app.bpartners.gen.annotator.endpoint.rest.api.JobsApi;
import app.bpartners.gen.annotator.endpoint.rest.client.ApiException;
import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.annotation.AnnotationRetrievingTask;
import app.bpartners.geojobs.repository.model.detection.*;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnnotationService {
  public static final int DEFAULT_IMAGES_HEIGHT = 1024;
  public static final int DEFAULT_IMAGES_WIDTH = 1024;
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d+");
  private final JobsApi jobsApi;
  private final TaskExtractor taskExtractor;
  private final LabelConverter labelConverter;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final AdminApi adminApi;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelConverter labelConverter,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      BucketComponent bucketComponent,
      EventProducer eventProducer) {
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

  @SneakyThrows
  public void createAnnotationJob(HumanDetectionJob humanDetectionJob, String jobName) {
    String folderPath = null;
    List<MachineDetectedTile> machineDetectedTiles = humanDetectionJob.getMachineDetectedTiles();
    log.info(
        "[DEBUG] AnnotationService detected tiles [size={}, tiles={}]",
        machineDetectedTiles.size(),
        machineDetectedTiles.stream().map(MachineDetectedTile::describe).toList());
    String annotationJobId = humanDetectionJob.getAnnotationJobId();
    List<DetectableObjectConfiguration> detectableObjects =
        humanDetectionJob.getDetectableObjectConfigurations();
    List<Label> expectedLabels =
        detectableObjects.stream()
            .map(object -> labelConverter.apply(object.getObjectType()))
            .toList();
    List<CreateAnnotatedTask> annotatedTasks =
        taskExtractor.apply(
            machineDetectedTiles, annotatorUserInfoGetter.getUserId(), expectedLabels);
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

  public List<AnnotationBatch> getAnnotations(String annotationJobId, String taskId) {
    try {
      int minPage = 1;
      int maxPageSize = 500;
      return adminApi.getAnnotationBatchesByJobTask(annotationJobId, taskId, minPage, maxPageSize);
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  public List<AnnotationRetrievingTask> retrieveTasksFromAnnotationJob(
      String humanZDJId,
      String retrievingJobId,
      String annotationJobId,
      Integer page,
      Integer pageSize,
      String userId) {
    List<Task> annotationTasks;
    try {
      annotationTasks =
          adminApi.getJobTasks(
              annotationJobId,
              page,
              pageSize,
              null, // So get any tasks either the status is
              userId); // page, pageSize and UserId not required
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
    return annotationTasks.stream()
        .map(
            annotationTask -> {
              var metadata = getTileMetaData(annotationTask.getFilename());
              var zoom = metadata.getFirst();
              var xTile = metadata.get(metadata.size() - 2);
              var yTile = metadata.getLast();
              // TODO: avoid redundant persisted metadata (x - y - z)
              return AnnotationRetrievingTask.builder()
                  .id(randomUUID().toString())
                  .jobId(retrievingJobId)
                  .annotationTaskId(annotationTask.getId())
                  .annotationJobId(annotationJobId)
                  .statusHistory(List.of())
                  .submissionInstant(Instant.now())
                  .humanZoneDetectionJobId(humanZDJId)
                  .zoom(zoom)
                  .xTile(xTile)
                  .yTile(yTile)
                  .build();
            })
        .collect(Collectors.toList());
  }

  private List<Integer> getTileMetaData(String filename) {
    var metadata = new ArrayList<Integer>();
    var filenameMatcher = DIGIT_PATTERN.matcher(filename);
    while (filenameMatcher.find()) {
      metadata.add(new BigInteger(filenameMatcher.group()).intValue());
    }
    int size = metadata.size();
    return metadata.subList(size - 3, size);
  }
}
