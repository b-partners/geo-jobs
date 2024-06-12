package app.bpartners.geojobs.service.tiling;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static app.bpartners.geojobs.repository.model.GeoJobType.TILING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.*;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.job.repository.JobStatusRepository;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.FilteredTilingJob;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFilteredMailer;
import app.bpartners.geojobs.service.NotFinishedTaskRetriever;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneTilingJobService extends JobService<TilingTask, ZoneTilingJob> {
  private final ZoneDetectionJobService detectionJobService;
  private final JobFilteredMailer<ZoneTilingJob> tilingFilteredMailer;
  private final NotFinishedTaskRetriever<TilingTask> notFinishedTaskRetriever;

  public ZoneTilingJobService(
      JpaRepository<ZoneTilingJob, String> repository,
      JobStatusRepository jobStatusRepository,
      TaskRepository<TilingTask> taskRepository,
      EventProducer eventProducer,
      ZoneDetectionJobService detectionJobService,
      JobFilteredMailer<ZoneTilingJob> tilingFilteredMailer,
      NotFinishedTaskRetriever<TilingTask> notFinishedTaskRetriever) {
    super(repository, jobStatusRepository, taskRepository, eventProducer, ZoneTilingJob.class);
    this.detectionJobService = detectionJobService;
    this.tilingFilteredMailer = tilingFilteredMailer;
    this.notFinishedTaskRetriever = notFinishedTaskRetriever;
  }

  public ZoneTilingJob importFromBucket(
      ZoneTilingJob job,
      String bucketName,
      String bucketPathPrefix,
      GeoServerParameter geoServerParameter,
      String geoServerUrl,
      Long startFrom,
      Long endAt) {
    var createdJob = repository.save(job);
    eventProducer.accept(
        List.of(
            ImportedZoneTilingJobSaved.builder()
                .startFrom(startFrom)
                .endAt(endAt)
                .jobId(createdJob.getId())
                .bucketName(bucketName)
                .bucketPathPrefix(bucketPathPrefix)
                .geoServerParameter(geoServerParameter)
                .geoServerUrl(geoServerUrl)
                .build()));
    return createdJob;
  }

  @Transactional
  public FilteredTilingJob dispatchTasksBySuccessStatus(String jobId) {
    ZoneTilingJob job = getZoneTilingJob(jobId);
    if (job.isSucceeded()) {
      throw new BadRequestException(
          "All job(id="
              + jobId
              + ") tasks are SUCCEEDED. "
              + "Use POST /tiling/id/duplications to duplicate job instead");
    }
    List<TilingTask> tilingTasks = parcelTaskDetectionTaskRepostiory.findAllByJobId(jobId);
    List<TilingTask> succeededTasks = tilingTasks.stream().filter(Task::isSucceeded).toList();
    List<TilingTask> notSucceededTasks =
        tilingTasks.stream().filter(task -> !task.isSucceeded()).toList();
    String succeededJobId = randomUUID().toString();
    String notSucceededJobId = randomUUID().toString();
    var succeededJob =
        duplicateWithNewStatus(
            succeededJobId,
            job,
            succeededTasks,
            true,
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
            notSucceededTasks,
            false,
            JobStatus.builder()
                .jobId(notSucceededJobId)
                .progression(PENDING)
                .health(UNKNOWN)
                .creationDatetime(now())
                .build());
    FilteredTilingJob filteredTilingJob =
        new FilteredTilingJob(jobId, succeededJob, notSucceededJob);
    tilingFilteredMailer.accept(filteredTilingJob);
    return filteredTilingJob;
  }

  @Transactional
  public TaskStatistic computeTaskStatistics(String jobId) {
    ZoneTilingJob job = getZoneTilingJob(jobId);
    eventProducer.accept(List.of(TaskStatisticRecomputingSubmitted.builder().jobId(jobId).build()));
    return TaskStatistic.builder()
        .jobId(jobId)
        .jobType(TILING)
        .actualJobStatus(job.getStatus())
        .taskStatusStatistics(List.of())
        .updatedAt(job.getStatus().getCreationDatetime())
        .build();
  }

  private ZoneTilingJob getZoneTilingJob(String jobId) {
    ZoneTilingJob job =
        repository
            .findById(jobId)
            .orElseThrow(() -> new NotFoundException("ZoneTilingJob(id=" + jobId + ") not found"));
    return job;
  }

  @Transactional
  public ZoneTilingJob retryFailedTask(String jobId) {
    ZoneTilingJob job = getZoneTilingJob(jobId);
    List<TilingTask> tilingTasks = parcelTaskDetectionTaskRepostiory.findAllByJobId(jobId);
    if (!tilingTasks.stream()
        .allMatch(task -> task.getStatus().getProgression().equals(FINISHED))) {
      throw new BadRequestException("Only job with all finished tasks can be retry");
    }
    List<TilingTask> failedTasks =
        tilingTasks.stream().filter(task -> task.getStatus().getHealth().equals(FAILED)).toList();
    if (failedTasks.isEmpty()) {
      throw new BadRequestException(
          "All tilling tasks of job(id=" + jobId + ") are already SUCCEEDED");
    }
    List<TilingTask> savedFailedTasks =
        parcelTaskDetectionTaskRepostiory.saveAll(
            failedTasks.stream().map(notFinishedTaskRetriever).toList());
    savedFailedTasks.forEach(task -> eventProducer.accept(List.of(new TilingTaskCreated(task))));
    // /!\ Force job status to status PROCESSING again
    job.hasNewStatus(
        JobStatus.builder()
            .id(randomUUID().toString())
            .jobId(jobId)
            .jobType(TILING)
            .progression(PROCESSING)
            .health(RETRYING)
            .creationDatetime(now())
            .build());
    return repository.save(job);
  }

  @Transactional
  @Override
  public ZoneTilingJob create(ZoneTilingJob job, List<TilingTask> tasks) {
    var saved = super.create(job, tasks);
    eventProducer.accept(List.of(new ZoneTilingJobCreated(saved)));
    return saved;
  }

  @Transactional
  public void fireTasks(ZoneTilingJob job) {
    getTasks(job).forEach(task -> eventProducer.accept(List.of(new TilingTaskCreated(task))));
  }

  @Override
  protected void onStatusChanged(ZoneTilingJob oldJob, ZoneTilingJob newJob) {
    eventProducer.accept(
        List.of(ZoneTilingJobStatusChanged.builder().oldJob(oldJob).newJob(newJob).build()));
  }

  @Transactional
  public ZoneTilingJob duplicate(String jobId) {
    String duplicatedJobId = randomUUID().toString();
    ZoneTilingJob originalJob = getZoneTilingJob(jobId);

    eventProducer.accept(
        List.of(
            ZoneTilingJobWithoutTasksCreated.builder()
                .duplicatedJobId(duplicatedJobId)
                .originalJob(originalJob)
                .build()));

    return originalJob.duplicate(duplicatedJobId);
  }

  public ZoneTilingJob duplicateWithNewStatus(
      String duplicatedJobId,
      ZoneTilingJob job,
      List<TilingTask> tasks,
      boolean saveZDJ,
      JobStatus newStatus) {
    List<TilingTask> duplicatedTasks =
        tasks.stream()
            .map(
                task -> {
                  var newTaskId = randomUUID().toString();
                  var newParcelId = randomUUID().toString();
                  var newParcelContentId = randomUUID().toString();
                  boolean hasSameStatuses = false;
                  boolean hasSameTile = false;
                  return task.duplicate(
                      newTaskId,
                      duplicatedJobId,
                      newParcelId,
                      newParcelContentId,
                      hasSameStatuses,
                      hasSameTile);
                })
            .toList();
    ZoneTilingJob jobToDuplicate = job.duplicate(duplicatedJobId);
    if (newStatus != null) {
      List<JobStatus> statusHistory = new ArrayList<>(jobToDuplicate.getStatusHistory());
      statusHistory.add(newStatus);
      jobToDuplicate.setStatusHistory(statusHistory);
    }
    ZoneTilingJob duplicatedJob = repository.save(jobToDuplicate);
    parcelTaskDetectionTaskRepostiory.saveAll(duplicatedTasks);
    if (saveZDJ) {
      detectionJobService.saveZDJFromZTJ(duplicatedJob);
    }
    return duplicatedJob;
  }
}
