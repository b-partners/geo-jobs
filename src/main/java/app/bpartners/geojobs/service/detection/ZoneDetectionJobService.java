package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.StatusMapper;
import app.bpartners.geojobs.endpoint.rest.model.GeoJsonsUrl;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.FilteredDetectionJob;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.annotator.AnnotationService;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final TileDetectionTaskRepository tileDetectionTaskRepository;
  private final DetectionFilteredMailer detectionFilteredMailer;

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
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      TileDetectionTaskRepository tileDetectionTaskRepository,
      DetectionFilteredMailer detectionFilteredMailer) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneDetectionJob.class);
    this.tilingTaskRepository = tilingTaskRepository;
    this.detectionMapper = detectionMapper;
    this.objectConfigurationRepository = objectConfigurationRepository;
    this.statusMapper = statusMapper;
    this.humanDetectionJobRepository = humanDetectionJobRepository;
    this.annotationService = annotationService;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
    this.tileDetectionTaskRepository = tileDetectionTaskRepository;
    this.detectionFilteredMailer = detectionFilteredMailer;
  }

  public TaskStatistic computeTaskStatistics(String jobId) {
    ZoneDetectionJob detectionJob = findById(jobId);
    eventProducer.accept(List.of(TaskStatisticRecomputingSubmitted.builder().jobId(jobId).build()));
    return TaskStatistic.builder()
        .jobId(jobId)
        .jobType(DETECTION)
        .actualJobStatus(detectionJob.getStatus())
        .taskStatusStatistics(List.of())
        .updatedAt(detectionJob.getStatus().getCreationDatetime())
        .build();
  }

  @Transactional
  public FilteredDetectionJob dispatchTasksBySucceededStatus(String jobId) {
    var job = findById(jobId);
    var tileDetectionTasks = tileDetectionTaskRepository.findAllByJobId(job.getId());
    var succeededTileTasks = tileDetectionTasks.stream().filter(Task::isSucceeded).toList();
    if (succeededTileTasks.size() == tileDetectionTasks.size()) {
      throw new BadRequestException(
          "All tile detection tasks of ZoneDetectionJob(id=" + jobId + ") are already SUCCEEDED");
    }
    var notSucceededTileTasks =
        tileDetectionTasks.stream()
            .filter(tileDetectionTask -> !tileDetectionTask.isSucceeded())
            .toList();
    var succeededJobId = randomUUID().toString();
    var notSucceededJobId = randomUUID().toString();

    var succeededDetectionTasks =
        getDetectionTasksFrom(succeededTileTasks, succeededJobId, FINISHED, SUCCEEDED);
    var notSucceededDetectionTasks =
        getDetectionTasksFrom(notSucceededTileTasks, notSucceededJobId, PENDING, UNKNOWN);

    var succeededJob =
        duplicateWithNewStatus(
            succeededJobId,
            job,
            succeededDetectionTasks,
            JobStatus.builder()
                .jobId(succeededJobId)
                .progression(FINISHED)
                .health(SUCCEEDED)
                .creationDatetime(now())
                .build());

    var notSucceededJob =
        duplicateWithNewStatus(
            notSucceededJobId,
            job,
            notSucceededDetectionTasks,
            JobStatus.builder()
                .jobId(succeededJobId)
                .progression(PENDING)
                .health(UNKNOWN)
                .creationDatetime(now())
                .build());

    var filteredDetectionJob = new FilteredDetectionJob(jobId, succeededJob, notSucceededJob);
    detectionFilteredMailer.accept(filteredDetectionJob);
    return filteredDetectionJob;
  }

  private List<DetectionTask> getDetectionTasksFrom(
      List<TileDetectionTask> tileDetectionTasks,
      String jobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    List<DetectionTask> detectionTasks = new ArrayList<>();
    Map<String, List<TileDetectionTask>> taskGroupedByParent =
        tileDetectionTasks.stream()
            .filter(task -> task.getParentTaskId() != null)
            .collect(Collectors.groupingBy(Task::getParentTaskId));
    for (Map.Entry<String, List<TileDetectionTask>> entry : taskGroupedByParent.entrySet()) {
      String parentTaskId = entry.getKey();
      List<TileDetectionTask> tileTasks = entry.getValue();
      List<Tile> tiles = tileTasks.stream().map(TileDetectionTask::getTile).toList();
      DetectionTask existingTask =
          taskRepository
              .findById(parentTaskId)
              .orElseThrow(
                  () -> new NotFoundException("DetectionTask(id=" + parentTaskId + ") not found"));
      String parcelId = randomUUID().toString();
      String parcelContentId = randomUUID().toString();
      String taskId = randomUUID().toString();
      Parcel duplicatedParcel =
          existingTask.getParcel().duplicate(parcelId, parcelContentId, tiles);

      detectionTasks.add(
          DetectionTask.builder()
              .id(taskId)
              .jobId(jobId)
              .submissionInstant(now())
              .statusHistory(
                  List.of(
                      TaskStatus.builder()
                          .health(healthStatus)
                          .progression(progressionStatus)
                          .creationDatetime(now())
                          .taskId(taskId)
                          .build()))
              .parcels(List.of(duplicatedParcel))
              .build());
    }
    return detectionTasks;
  }

  private ZoneDetectionJob duplicateWithNewStatus(
      String duplicatedJobId,
      ZoneDetectionJob job,
      List<DetectionTask> tasks,
      JobStatus newStatus) {
    ZoneDetectionJob jobToDuplicate = job.duplicate(duplicatedJobId);
    if (newStatus != null) {
      List<JobStatus> statusHistory = new ArrayList<>(jobToDuplicate.getStatusHistory());
      statusHistory.add(newStatus);
      jobToDuplicate.setStatusHistory(statusHistory);
    }
    ZoneDetectionJob duplicatedJob = repository.save(jobToDuplicate);
    taskRepository.saveAll(tasks);
    return duplicatedJob;
  }

  @Transactional
  public ZoneDetectionJob retryFailedTask(String jobId) {
    ZoneDetectionJob job =
        repository
            .findById(jobId)
            .orElseThrow(
                () -> new NotFoundException("ZoneDetectionJob(id=" + jobId + ") not found"));
    List<DetectionTask> detectionTasks = taskRepository.findAllByJobId(jobId);

    List<DetectionTask> unfinishedTasks = new ArrayList<>();
    List<DetectionTask> finishedWithBadStatus = new ArrayList<>();
    detectionTasks.forEach(
        task -> {
          var tileDetectionTasks = tileDetectionTaskRepository.findAllByParentTaskId(task.getId());
          boolean computedStatusIsSucceeded =
              tileDetectionTasks.stream()
                  .allMatch(
                      tileDetectionTask ->
                          tileDetectionTask.getStatus().getProgression().equals(FINISHED)
                              && tileDetectionTask.getStatus().getHealth().equals(SUCCEEDED));
          if (!computedStatusIsSucceeded) unfinishedTasks.add(task);
          var taskStatus = task.getStatus();
          boolean savedStatusIsSucceeded =
              taskStatus.getProgression().equals(FINISHED)
                  && taskStatus.getHealth().equals(SUCCEEDED);
          if (computedStatusIsSucceeded && !savedStatusIsSucceeded) finishedWithBadStatus.add(task);
        });

    var optionalFinishedJob = computeFromDetectionBadStatuses(finishedWithBadStatus, job);
    if (optionalFinishedJob.isPresent()) {
      return optionalFinishedJob.get();
    }

    if (unfinishedTasks.isEmpty()) {
      throw new BadRequestException(
          "All tilling tasks of job(id=" + jobId + ") are already SUCCEEDED");
    }
    List<DetectionTask> savedFailedTasks =
        taskRepository.saveAll(
            unfinishedTasks.stream()
                .peek(
                    failedTask -> {
                      List<TaskStatus> newStatus = new ArrayList<>(failedTask.getStatusHistory());
                      newStatus.add(
                          TaskStatus.builder()
                              .id(randomUUID().toString())
                              .taskId(failedTask.getId())
                              .jobType(DETECTION)
                              .progression(PENDING)
                              .health(RETRYING)
                              .creationDatetime(now())
                              .build());
                      failedTask.setStatusHistory(newStatus);
                    })
                .toList());
    savedFailedTasks.forEach(task -> eventProducer.accept(List.of(new DetectionTaskCreated(task))));
    // /!\ Force job status to status PROCESSING again
    job.hasNewStatus(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(DETECTION)
            .progression(PROCESSING)
            .health(RETRYING)
            .creationDatetime(now())
            .build());
    return repository.save(job);
  }

  private Optional<ZoneDetectionJob> computeFromDetectionBadStatuses(
      List<DetectionTask> finishedWithBadStatus, ZoneDetectionJob initialJob) {
    if (finishedWithBadStatus.isEmpty()) return Optional.empty();
    finishedWithBadStatus.forEach(
        task -> {
          List<TaskStatus> newStatus = new ArrayList<>(task.getStatusHistory());
          newStatus.add(
              TaskStatus.builder()
                  .id(randomUUID().toString())
                  .taskId(task.getId())
                  .jobType(DETECTION)
                  .progression(FINISHED)
                  .health(SUCCEEDED)
                  .creationDatetime(now())
                  .build());
          task.setStatusHistory(newStatus);
          taskRepository.save(task);
        });
    // This method recompute job new status and send StatusChanged event
    ZoneDetectionJob newFinishedJob = this.recomputeStatus(initialJob);
    if (newFinishedJob.getStatus().getProgression().equals(FINISHED)
        && newFinishedJob.getStatus().getHealth().equals(SUCCEEDED)) {
      return Optional.of(newFinishedJob);
    }
    return Optional.empty();
  }

  public GeoJsonsUrl getGeoJsonsUrl(String jobId) {
    var humanZDJ = getHumanZdjFromZdjId(jobId);
    var jobStatus = humanZDJ.getStatus();

    return new GeoJsonsUrl()
        .url(generateGeoJsonsUrl(jobStatus))
        .status(statusMapper.toRest(jobStatus));
  }

  private String generateGeoJsonsUrl(JobStatus jobStatus) {
    if (!jobStatus.getProgression().equals(FINISHED)
        && !jobStatus.getHealth().equals(Status.HealthStatus.SUCCEEDED)) {
      log.info(
          "Unable to generate geoJsons Url to unfinished succeeded job. Actual status is "
              + jobStatus);
      return null;
    }
    return "NotImplemented: finished human detection job without url";
  }

  public ZoneDetectionJob checkHumanDetectionJobStatus(String jobId) {
    var humanZDJ = getHumanZdjFromZdjId(jobId);
    var humanDetectionJobs = humanDetectionJobRepository.findByZoneDetectionJobId(humanZDJ.getId());
    if (humanDetectionJobs.isEmpty()) return humanZDJ;

    var firstHumanDetectionJob = humanDetectionJobs.getFirst();
    var lastHumanDetectionJob = humanDetectionJobs.getLast();
    var firstAnnotationJobStatus =
        annotationService
            .getAnnotationJobById(firstHumanDetectionJob.getAnnotationJobId())
            .getStatus();
    Status.ProgressionStatus firstProgressionStatus =
        detectionMapper.getProgressionStatus(firstAnnotationJobStatus);
    Status.HealthStatus firstHealthStatus =
        detectionMapper.getHealthStatus(firstAnnotationJobStatus);
    var lastAnnotationJobStatus =
        annotationService
            .getAnnotationJobById(lastHumanDetectionJob.getAnnotationJobId())
            .getStatus();
    Status.ProgressionStatus lastProgressionStatus =
        detectionMapper.getProgressionStatus(lastAnnotationJobStatus);
    Status.HealthStatus lastHealthStatus = detectionMapper.getHealthStatus(lastAnnotationJobStatus);

    Status.ProgressionStatus humanZDJProgression;
    Status.HealthStatus humanZDJHealth;
    if (firstProgressionStatus.equals(lastProgressionStatus)
        && firstHealthStatus.equals(lastHealthStatus)) {
      humanZDJProgression = firstProgressionStatus;
      humanZDJHealth = firstHealthStatus;
    } else {
      humanZDJProgression = PENDING; // TODO: check when processing
      humanZDJHealth = UNKNOWN;
    }

    humanZDJ.hasNewStatus(
        Status.builder()
            .id(randomUUID().toString())
            .progression(humanZDJProgression)
            .health(humanZDJHealth)
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
      String jobId, List<DetectableObjectConfiguration> objectConfigurationsFromMachineZDJ) {
    var job = findById(jobId);
    var humanZDJ = this.getHumanZdjFromZdjId(jobId);
    var humanZDJId = humanZDJ.getId();
    var objectConfigurationsFromHumanZDJ =
        objectConfigurationsFromMachineZDJ.stream()
            .map(objectConf -> objectConf.duplicate(randomUUID().toString(), humanZDJId))
            .toList();
    objectConfigurationRepository.saveAll(
        Stream.of(objectConfigurationsFromMachineZDJ, objectConfigurationsFromHumanZDJ)
            .flatMap(List::stream)
            .toList());
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
                  var generatedTaskId = randomUUID().toString();
                  DetectionTask detectionTask = new DetectionTask();
                  detectionTask.setId(generatedTaskId);
                  detectionTask.setJobId(zoneDetectionJob.getId());
                  detectionTask.setParcels(parcels);
                  detectionTask.setStatusHistory(
                      List.of(
                          TaskStatus.builder()
                              .id(randomUUID().toString())
                              .taskId(generatedTaskId)
                              .health(UNKNOWN)
                              .progression(PENDING)
                              .creationDatetime(now())
                              .build()));
                  detectionTask.setSubmissionInstant(now());
                  return detectionTask;
                })
            .toList();

    return super.create(zoneDetectionJob, detectionTasks);
  }

  public ZoneDetectionJob save(ZoneDetectionJob job) {
    return repository.save(job);
  }
}
