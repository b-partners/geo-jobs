package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
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
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ZoneDetectionJobService extends JobService<DetectionTask, ZoneDetectionJob> {
  private final DetectionMapper detectionMapper;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final TilingTaskRepository tilingTaskRepository;
  private final StatusMapper<JobStatus> statusMapper;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TilingTaskRepository tilingTaskRepository,
      DetectionTaskRepository taskRepository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper,
      DetectableObjectConfigurationRepository objectConfigurationRepository,
      StatusMapper<JobStatus> statusMapper) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneDetectionJob.class);
    this.tilingTaskRepository = tilingTaskRepository;
    this.detectionMapper = detectionMapper;
    this.objectConfigurationRepository = objectConfigurationRepository;
    this.statusMapper = statusMapper;
  }

  public GeoJsonsUrl getGeoJsonsUrl(String jobId) {
    var optionalZoneDetectionJob = repository.findById(jobId);
    if (optionalZoneDetectionJob.isEmpty()) {
      throw new NotFoundException("ZoneDetectionJob(id=" + jobId + ") is not found");
    }
    var zoneDetectionJob = optionalZoneDetectionJob.get();
    var jobStatus = zoneDetectionJob.getStatus();

    return new GeoJsonsUrl()
        .url(generateGeoJsonsUrl(jobStatus))
        .status(statusMapper.statusConverter(jobStatus));
  }

  private String generateGeoJsonsUrl(JobStatus jobStatus) {
    if (!jobStatus.getProgression().equals(FINISHED)
        && !jobStatus.getHealth().equals(Status.HealthStatus.SUCCEEDED)) {
      log.warn(
          "Unable to generate geoJsons Url to unfinished succeeded job. Actual status is "
              + jobStatus);
      return null;
    }
    return "NotImplemented: finished human detection job without url";
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
                              .health(UNKNOWN)
                              .progression(PENDING)
                              .creationDatetime(now())
                              .taskId(generatedTaskId)
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
