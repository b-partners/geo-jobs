package app.bpartners.geojobs.service.event;

import static app.bpartners.annotator.endpoint.rest.model.JobStatus.TO_REVIEW;
import static app.bpartners.annotator.endpoint.rest.model.JobType.REVIEWING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.annotator.endpoint.rest.api.AnnotatedJobsApi;
import app.bpartners.annotator.endpoint.rest.model.AnnotatedTask;
import app.bpartners.annotator.endpoint.rest.model.CrupdateAnnotatedJob;
import app.bpartners.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import app.bpartners.geojobs.service.annotator.AnnotatorApiConf;
import app.bpartners.geojobs.service.annotator.AnnotatorUserInfoGetter;
import app.bpartners.geojobs.service.annotator.LabelExtractor;
import app.bpartners.geojobs.service.annotator.TaskExtractor;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class ZoneDetectionJobSucceededService implements Consumer<ZoneDetectionJobSucceeded> {
  private final DetectedTileRepository detectedTileRepository;
  private AnnotatedJobsApi annotatedJobsApi;
  private final TaskExtractor taskExtractor;
  private final LabelExtractor labelExtractor;
  private final AnnotatorUserInfoGetter annotatorUserInfoGetter;
  private final BucketComponent bucketComponent;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;

  public ZoneDetectionJobSucceededService(
      DetectedTileRepository detectedTileRepository,
      AnnotatorApiConf annotatorApiConf,
      TaskExtractor taskExtractor,
      LabelExtractor labelExtractor,
      AnnotatorUserInfoGetter annotatorUserInfoGetter,
      BucketComponent bucketComponent,
      DetectableObjectConfigurationRepository objectConfigurationRepository) {
    this.detectedTileRepository = detectedTileRepository;
    this.annotatedJobsApi = new AnnotatedJobsApi(annotatorApiConf.newApiClientWithApiKey());
    this.taskExtractor = taskExtractor;
    this.labelExtractor = labelExtractor;
    this.annotatorUserInfoGetter = annotatorUserInfoGetter;
    this.bucketComponent = bucketComponent;
    this.objectConfigurationRepository = objectConfigurationRepository;
  }

  public ZoneDetectionJobSucceededService annotatedJobsApi(AnnotatedJobsApi annotatedJobsApi) {
    this.annotatedJobsApi = annotatedJobsApi;
    return this;
  }

  @Override
  public void accept(ZoneDetectionJobSucceeded event) {
    String jobId = event.getJobId();
    // TODO: process human ZDJ here and update status

    try {
      sendAnnotations(jobId);
    } catch (Exception e) {
      throw new ApiException(SERVER_EXCEPTION, e); // TODO: retry with some attempts ?
    }
  }

  private void sendAnnotations(String jobId)
      throws app.bpartners.annotator.endpoint.rest.client.ApiException {
    List<DetectedTile> detectedTiles = detectedTileRepository.findAllByJobId(jobId);
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        objectConfigurationRepository.findAllByDetectionJobId(jobId);
    List<DetectedTile> detectedInDoubtTiles =
        detectedTiles.stream()
            .filter(
                detectedTile ->
                    detectedTile.getDetectedObjects().stream()
                        .anyMatch(
                            detectedObject ->
                                detectedObject.isInDoubt(detectableObjectConfigurations)))
            .toList();
    String crupdateAnnotatedJobId = randomUUID().toString();
    String crupdateAnnotatedJobFolderPath = "/"; // TODO: can this be null ?
    List<AnnotatedTask> annotatedTasks =
        taskExtractor.apply(detectedInDoubtTiles, annotatorUserInfoGetter.getUserId());
    List<Label> labels = labelExtractor.extractLabelsFromTasks(annotatedTasks);
    String crupdateJobId = randomUUID().toString();
    Instant now = Instant.now();

    annotatedJobsApi.crupdateAnnotatedJob(
        crupdateJobId,
        new CrupdateAnnotatedJob()
            .id(crupdateAnnotatedJobId)
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
