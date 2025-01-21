package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.*;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationJobVerificationSent;
import app.bpartners.geojobs.endpoint.event.model.parcel.ParcelDetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.model.status.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobStatusChanged;
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
      TaskStatisticRepository taskStatisticRepository) {
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
  }

  @Transactional
  public ZoneDetectionJob processZDJ(
      String jobId, List<DetectableObjectConfiguration> configurations) {
    return fireTasks(jobId, configurations);
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
  public ZoneDetectionJob getByTilingJobId(
      String tilingJobId, ZoneDetectionJob.DetectionType detectionType) {
    var jobs =
        zoneDetectionJobRepository.findAllByZoneTilingJob_Id(tilingJobId).stream()
            .filter(job -> job.getDetectionType().equals(detectionType))
            .toList();
    var retrievedJob = jobs.getFirst();
    if (jobs.size() > 1) {
      log.error("ZTJ(id={}) associated to {} ZDJ", tilingJobId, jobs.size());
    }
    return retrievedJob;
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

  public ZoneDetectionJob fireTasks(String jobId) {
    var job = findById(jobId);
    getTasks(job)
        .forEach(task -> eventProducer.accept(List.of(new ParcelDetectionTaskCreated(task))));
    eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(job.getId())));
    eventProducer.accept(List.of(new AutoTaskStatisticRecomputingSubmitted(job.getId())));
    return job;
  }

  @Transactional
  public ZoneDetectionJob fireTasks(
      String jobId, List<DetectableObjectConfiguration> objectConfigurationsFromMachineZDJ) {
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

    return fireTasks(jobId);
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
    repository.save(savedZDJ.toBuilder().id(randomUUID().toString()).detectionType(HUMAN).build());
    return savedZDJ;
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
