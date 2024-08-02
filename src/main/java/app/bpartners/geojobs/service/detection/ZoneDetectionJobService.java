package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.*;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.FilteredDetectionJob;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ZoneDetectionJobService extends JobService<ParcelDetectionTask, ZoneDetectionJob> {
  private final DetectionMapper detectionMapper;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final TilingTaskRepository tilingTaskRepository;
  private final HumanDetectionJobRepository humanDetectionJobRepository;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final DetectableObjectConfigurationMapper objectConfigurationMapper;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TilingTaskRepository tilingTaskRepository,
      ParcelDetectionTaskRepository taskRepository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper,
      DetectableObjectConfigurationRepository objectConfigurationRepository,
      HumanDetectionJobRepository humanDetectionJobRepository,
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      TaskStatisticRepository taskStatisticRepository,
      DetectableObjectConfigurationMapper objectConfigurationMapper) {
    super(
        repository,
        jobStatusRepository,
        taskStatisticRepository,
        taskRepository,
        eventProducer,
        ZoneDetectionJob.class);
    this.tilingTaskRepository = tilingTaskRepository;
    this.detectionMapper = detectionMapper;
    this.objectConfigurationRepository = objectConfigurationRepository;
    this.humanDetectionJobRepository = humanDetectionJobRepository;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
    this.objectConfigurationMapper = objectConfigurationMapper;
  }

  @Transactional
  public ZoneDetectionJob processZDJ(
      String jobId,
      List<app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration>
          detectableObjectConfigurations) {
    List<app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration>
        configurations =
            detectableObjectConfigurations.stream()
                .map(objectConf -> objectConfigurationMapper.toDomain(jobId, objectConf))
                .toList();
    return fireTasks(jobId, configurations);
  }

  @Transactional
  public FilteredDetectionJob dispatchTasksBySucceededStatus(String jobId) {
    throw new NotImplementedException("Not supported");
    /*
    TODO
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
    return filteredDetectionJob;*/
  }

  private List<ParcelDetectionTask> getDetectionTasksFrom(
      List<TileDetectionTask> tileDetectionTasks,
      String zoneDetectionJobId,
      Status.ProgressionStatus progressionStatus,
      Status.HealthStatus healthStatus) {
    List<ParcelDetectionTask> detectionTasks = new ArrayList<>();
    Map<String, List<TileDetectionTask>> taskGroupedByParent =
        tileDetectionTasks.stream()
            .filter(task -> task.getJobId() != null)
            .collect(Collectors.groupingBy(Task::getJobId));
    for (Map.Entry<String, List<TileDetectionTask>> entry : taskGroupedByParent.entrySet()) {
      String parentTaskId = entry.getKey();
      List<TileDetectionTask> tileTasks = entry.getValue();
      List<Tile> tiles = tileTasks.stream().map(TileDetectionTask::getTile).toList();
      ParcelDetectionTask existingTask =
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
          ParcelDetectionTask.builder()
              .id(taskId)
              .jobId(zoneDetectionJobId)
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
      List<ParcelDetectionTask> tasks,
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
    // TODO: not implemented
    throw new NotImplementedException("Not supported");
    /*ZoneDetectionJob job =
        repository
            .findById(jobId)
            .orElseThrow(
                () -> new NotFoundException("ZoneDetectionJob(id=" + jobId + ") not found"));
    List<ParcelDetectionTask> parcelDetectionTasks = taskRepository.findAllByJobId(jobId);

    List<ParcelDetectionTask> unfinishedTasks = new ArrayList<>();
    List<ParcelDetectionTask> finishedWithBadStatus = new ArrayList<>();
    parcelDetectionTasks.forEach(
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
    List<ParcelDetectionTask> savedFailedTasks =
        taskRepository.saveAll(unfinishedTasks.stream().map(notFinishedTaskRetriever).toList());
    savedFailedTasks.forEach(
        task -> eventProducer.accept(List.of(new ParcelDetectionTaskCreated(task))));
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
    return repository.save(job);*/
  }

  private Optional<ZoneDetectionJob> computeFromDetectionBadStatuses(
      List<ParcelDetectionTask> finishedWithBadStatus, ZoneDetectionJob initialJob) {
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

  @Transactional
  public ZoneDetectionJob checkHumanDetectionJobStatus(String annotationJobId) {
    var linkedHumanDetectionJob =
        humanDetectionJobRepository
            .findByAnnotationJobId(annotationJobId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "No human detection job found for annotationJobId=" + annotationJobId));
    var humanZDJ = getHumanZdjFromZdjId(linkedHumanDetectionJob.getZoneDetectionJobId());
    eventProducer.accept(List.of(new AnnotationJobVerificationSent(humanZDJ.getId())));
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
    getTasks(job)
        .forEach(task -> eventProducer.accept(List.of(new ParcelDetectionTaskCreated(task))));
    return job;
  }

  @Transactional
  public ZoneDetectionJob fireTasks(
      String jobId, List<DetectableObjectConfiguration> objectConfigurationsFromMachineZDJ) {
    var job = findById(jobId);
    var humanZDJ = this.getHumanZdjFromZdjId(jobId);
    var humanZDJId = humanZDJ.getId();
    Set<String> bucketStorageNameCollections =
        objectConfigurationsFromMachineZDJ.stream()
            .map(DetectableObjectConfiguration::getBucketStorageName)
            .collect(Collectors.toSet());
    if (!bucketStorageNameCollections.isEmpty() && bucketStorageNameCollections.size() != 1) {
      throw new NotImplementedException("Only same detectable is supported for now");
    }
    var objectConfigurationsFromHumanZDJ =
        objectConfigurationsFromMachineZDJ.stream()
            .map(objectConf -> objectConf.duplicate(randomUUID().toString(), humanZDJId))
            .toList();
    objectConfigurationRepository.saveAll(
        Stream.of(objectConfigurationsFromMachineZDJ, objectConfigurationsFromHumanZDJ)
            .flatMap(List::stream)
            .toList());
    getTasks(job)
        .forEach(task -> eventProducer.accept(List.of(new ParcelDetectionTaskCreated(task))));

    eventProducer.accept(List.of(new ZDJParcelsStatusRecomputingSubmitted(job.getId())));
    eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(job.getId())));
    eventProducer.accept(List.of(new AutoTaskStatisticRecomputingSubmitted(job.getId())));
    return job;
  }

  @Override
  protected void onStatusChanged(ZoneDetectionJob oldJob, ZoneDetectionJob newJob) {
    eventProducer.accept(
        List.of(ZoneDetectionJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  @Transactional
  public ZoneDetectionJob saveZDJFromZTJ(ZoneTilingJob job) {
    var zoneDetectionJob = detectionMapper.fromTilingJob(job);
    var tilingTasks = tilingTaskRepository.findAllByJobId(job.getId());

    var savedZDJ = saveWithTasks(tilingTasks, zoneDetectionJob);
    return repository.save(
        savedZDJ.toBuilder().id(randomUUID().toString()).detectionType(HUMAN).build());
  }

  @Transactional
  public HumanDetectionJob getHumanDetectionJobById(String jobId) {
    return humanDetectionJobRepository
        .findById(jobId)
        .orElseThrow(() -> new NotFoundException("HumanDetectionJob(id=" + jobId + ") not found"));
  }

  public ZoneDetectionJob saveWithTasks(
      List<TilingTask> tilingTasks, ZoneDetectionJob zoneDetectionJob) {
    List<ParcelDetectionTask> parcelDetectionTasks =
        tilingTasks.stream()
            .map(
                tilingTask -> {
                  var parcels = tilingTask.getParcels();
                  var generatedTaskId = randomUUID().toString();
                  ParcelDetectionTask parcelDetectionTask = new ParcelDetectionTask();
                  parcelDetectionTask.setId(generatedTaskId);
                  parcelDetectionTask.setJobId(zoneDetectionJob.getId());
                  parcelDetectionTask.setParcels(parcels);
                  parcelDetectionTask.setStatusHistory(
                      List.of(
                          TaskStatus.builder()
                              .id(randomUUID().toString())
                              .taskId(generatedTaskId)
                              .health(UNKNOWN)
                              .progression(PENDING)
                              .creationDatetime(now())
                              .build()));
                  parcelDetectionTask.setSubmissionInstant(now());
                  return parcelDetectionTask;
                })
            .toList();

    return super.create(zoneDetectionJob, parcelDetectionTasks);
  }

  public ZoneDetectionJob save(ZoneDetectionJob job) {
    return repository.save(job);
  }
}
