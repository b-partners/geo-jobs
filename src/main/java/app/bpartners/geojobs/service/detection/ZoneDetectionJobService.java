package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.DetectionTaskCreated;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionTaskRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneDetectionJobService extends JobService<DetectionTask, ZoneDetectionJob> {
  private final DetectionMapper detectionMapper;
  private final DetectableObjectConfigurationRepository objectConfigurationRepository;
  private final TilingTaskRepository tilingTaskRepository;
  ;

  public ZoneDetectionJobService(
      JpaRepository<ZoneDetectionJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TilingTaskRepository tilingTaskRepository,
      DetectionTaskRepository taskRepository,
      EventProducer eventProducer,
      DetectionMapper detectionMapper,
      DetectableObjectConfigurationRepository objectConfigurationRepository) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneDetectionJob.class);
    this.tilingTaskRepository = tilingTaskRepository;
    this.detectionMapper = detectionMapper;
    this.objectConfigurationRepository = objectConfigurationRepository;
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
    var savedZDJ = repository.save(zoneDetectionJob);
    repository.save(savedZDJ.toBuilder().detectionType(HUMAN).build());

    var detectionJobId = savedZDJ.getId();
    var tilingTasks = tilingTaskRepository.findAllByJobId(job.getId());

    List<DetectionTask> detectionTasks =
        tilingTasks.stream()
            .map(
                tilingTask -> {
                  var parcels = tilingTask.getParcels();
                  var generatedTaskId = randomUUID().toString();
                  DetectionTask detectionTask = new DetectionTask();
                  detectionTask.setId(generatedTaskId);
                  detectionTask.setJobId(detectionJobId);
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

    super.create(savedZDJ, detectionTasks);
  }

  public ZoneDetectionJob save(ZoneDetectionJob job) {
    return repository.save(job);
  }
}
