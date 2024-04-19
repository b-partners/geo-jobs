package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ZoneDetectionJobService extends JobService<DetectionTask, ZoneDetectionJob> {
  private final DetectionMapper detectionMapper;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final TilingTaskRepository tilingTaskRepository;
  private final StatusMapper<JobStatus> statusMapper;
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final AnnotationService annotationService;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TilingTaskRepository tilingTaskRepository,
      DetectionTaskRepository taskRepository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper,
      DetectableObjectConfigurationRepository objectConfigurationRepository,
      StatusMapper<JobStatus> statusMapper,
      HumanDetectionJobRepository humanDetectionJobRepository,
      AnnotationService annotationService,
      ZoneDetectionJobRepository zoneDetectionJobRepository) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneDetectionJob.class);
    this.tilingTaskRepository = tilingTaskRepository;
    this.detectionMapper = detectionMapper;
    this.objectConfigurationRepository = objectConfigurationRepository;
    this.statusMapper = statusMapper;
    this.humanDetectionJobRepository = humanDetectionJobRepository;
    this.annotationService = annotationService;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
  }

  public GeoJsonsUrl getGeoJsonsUrl(String jobId) {
    var humanZDJ = getHumanZdjFromZdjId(jobId);
    var jobStatus = humanZDJ.getStatus();

    return new GeoJsonsUrl()
        .url(generateGeoJsonsUrl(jobStatus))
        .status(statusMapper.statusConverter(jobStatus));
  }

  private String generateGeoJsonsUrl(JobStatus jobStatus) {
    if (!jobStatus.getProgression().equals(FINISHED)
        && !jobStatus.getHealth().equals(Status.HealthStatus.SUCCEEDED)) {
      log.error(
          "Unable to generate geoJsons Url to unfinished succeeded job. Actual status is "
              + jobStatus);
      return null;
    }
    return "NotImplemented: finished human detection job without url";
  }

  public ZoneDetectionJob checkHumanDetectionJobStatus(String jobId) {
    var humanZDJ = getHumanZdjFromZdjId(jobId);
    var humanDetectionJob =
        humanDetectionJobRepository.findByZoneDetectionJobId(humanZDJ.getId()).orElse(null);
    if (humanDetectionJob == null) return humanZDJ;
    var annotationJobStatus =
        annotationService.getAnnotationJobById(humanDetectionJob.getAnnotationJobId()).getStatus();

    humanZDJ.hasNewStatus(
        Status.builder()
            .id(randomUUID().toString())
            .progression(detectionMapper.getProgressionStatus(annotationJobStatus))
            .health(detectionMapper.getHealthStatus(annotationJobStatus))
            .creationDatetime(now())
            .build());
    return repository.save(humanZDJ);
  }

  @Transactional
  public ZoneDetectionJob getHumanZdjFromZdjId(String jobId) {
    var zoneDetectionJob =
        repository
            .findById(jobId)
            .orElseThrow(
                () -> new NotFoundException("ZoneDetectionJob(id=" + jobId + ") not found"));
    if (zoneDetectionJob.getDetectionType() == HUMAN) {
      return zoneDetectionJob;
    }
    var associatedZdj =
        zoneDetectionJobRepository.findAllByZoneTilingJob_Id(
            zoneDetectionJob.getZoneTilingJob().getId());
    return associatedZdj.stream()
        .filter(job -> job.getDetectionType() == ZoneDetectionJob.DetectionType.HUMAN)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "ZoneDetectionJob(id="
                        + jobId
                        + ", type=MACHINE) is not associated to any"
                        + " ZoneDetectionJob.type=HUMAN"));
  }

  @Transactional
  public ZoneDetectionJob fireTasks(String jobId) {
    var job = findById(jobId);
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new DetectionTaskCreated(task))));
    return job;
  }

  @Transactional
  public ZoneDetectionJob fireTasks(
      String jobId, List<DetectableObjectConfiguration> objectConfigurations) {
    var job = findById(jobId);
    objectConfigurationRepository.saveAll(objectConfigurations);
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new DetectionTaskCreated(task))));
    return job;
  }

  @Override
  protected void onStatusChanged(ZoneDetectionJob oldJob, ZoneDetectionJob newJob) {
    eventProducer.accept(
        List.of(ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  @Transactional
  public void saveZDJFromZTJ(ZoneTilingJob job) {
    var zoneDetectionJob = detectionMapper.fromTilingJob(job);
    var tilingTasks = tilingTaskRepository.findAllByJobId(job.getId());

    var savedZDJ = saveWithTasks(tilingTasks, zoneDetectionJob);
    repository.save(savedZDJ.toBuilder().id(randomUUID().toString()).detectionType(HUMAN).build());
  }

  @Transactional
  public HumanDetectionJob getHumanDetectionJobById(String jobId) {
    return humanDetectionJobRepository
        .findById(jobId)
        .orElseThrow(() -> new NotFoundException("HumanDetectionJob(id=" + jobId + ") not found"));
  }

  public ZoneDetectionJob saveWithTasks(
      List<TilingTask> tilingTasks, ZoneDetectionJob zoneDetectionJob) {
    List<DetectionTask> detectionTasks =
        tilingTasks.stream()
            .map(
                tilingTask -> {
                  var parcels = tilingTask.getParcels();
                  log.info("[DEBUG] TilingTask Parcels {}", parcels);
                  var generatedTaskId = randomUUID().toString();
                  DetectionTask detectionTask = new DetectionTask();
                  detectionTask.setId(generatedTaskId);
                  detectionTask.setJobId(zoneDetectionJob.getId());
                  detectionTask.setParcels(parcels);
                  detectionTask.setStatusHistory(
                      List.of(
                          TaskStatus.builder()
                              .health(UNKNOWN)
                              .progression(PENDING)
                              .creationDatetime(now())
                              .taskId(generatedTaskId)
                              .build()));
                  detectionTask.setSubmissionInstant(now());
                  log.info("[DEBUG] DetectionTask Parcels {}", detectionTask.getParcels());
                  return detectionTask;
                })
            .toList();

    return super.create(zoneDetectionJob, detectionTasks);
  }

  public ZoneDetectionJob save(ZoneDetectionJob job) {
    return repository.save(job);
  }
}
