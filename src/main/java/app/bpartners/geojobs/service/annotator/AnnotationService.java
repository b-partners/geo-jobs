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
import app.bpartners.geojobs.endpoint.event.model.AnnotationBatchRetrievingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.CreateAnnotatedTaskSubmitted;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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
  private final DetectableObjectConfigurationRepository detectableObjectRepository;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final AdminApi adminApi;

  public AnnotationService(
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelConverter labelConverter,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      DetectableObjectConfigurationRepository detectableObjectRepository,
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      BucketComponent bucketComponent,
      EventProducer eventProducer) {
    this.jobsApi = new JobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.adminApi = new AdminApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelConverter = labelConverter;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.detectableObjectRepository = detectableObjectRepository;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
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
      return adminApi.getAnnotationBatchesByJobTask(
          annotationJobId, taskId, null, null); // page, pageSize not required
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  public void fireTasks(String jobId, String annotationJobId, int imageSize) {
    List<Task> annotationTasks;
    Integer page = null;
    Integer pageSize = null;
    String userId = null;
    try {
      annotationTasks =
          adminApi.getJobTasks(
              annotationJobId,
              page,
              pageSize,
              TaskStatus.COMPLETED,
              userId); // page, pageSize and UserId not required
    } catch (ApiException e) {
      throw new app.bpartners.geojobs.model.exception.ApiException(
          app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
    annotationTasks.forEach(
        task -> {
          var metadata = getTileMetaData(task.getFilename());
          var zoom = metadata.getFirst();
          var xTile = metadata.get(metadata.size() - 2);
          var yTile = metadata.getLast();
          eventProducer.accept(
              List.of(
                  new AnnotationBatchRetrievingSubmitted(
                      jobId, annotationJobId, task.getId(), imageSize, xTile, yTile, zoom)));
        });
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
