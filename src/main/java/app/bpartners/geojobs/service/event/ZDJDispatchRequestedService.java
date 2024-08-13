package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.model.ZDJDispatchRequested;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.model.TaskStatus;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.ParcelDetectionTaskRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.FilteredDetectionJob;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.JobFilteredMailer;
import app.bpartners.geojobs.service.KeyPredicateFunction;
import app.bpartners.geojobs.service.TaskToJobConverter;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZDJDispatchRequestedService implements Consumer<ZDJDispatchRequested> {
  private final ZoneDetectionJobRepository zdjRepository;
  private final ParcelDetectionTaskRepository parcelDetectionTaskRepository;
  private final JobFilteredMailer<ZoneDetectionJob> detectionFilteredMailer;
  private final TaskToJobConverter<ParcelDetectionTask, ParcelDetectionJob> parcelJobConverter;
  private final KeyPredicateFunction keyPredicateFunction;
  private final ParcelDetectionJobService parcelDetectionJobService;
  private final TileDetectionTaskRepository tileDetectionTaskRepository;

  @Override
  public void accept(ZDJDispatchRequested event) {
    var job = event.getJob();
    var jobId = job.getId();
    var succeededJobId = event.getSucceededJobId();
    var notSucceededJobId = event.getNotSucceededJobId();

    var parcelDetectionTasks = parcelDetectionTaskRepository.findAllByJobId(jobId);
    log.info("[DEBUG] all PDT size {}", parcelDetectionTasks.size());
    var succeededParcelDetectionTasks =
        parcelDetectionTasks.stream().filter(Task::isSucceeded).toList();
    log.info("[DEBUG] succeeded PDT size {}", succeededParcelDetectionTasks.size());
    var notSucceededParcelDetectionTasks =
        parcelDetectionTasks.stream().filter(task -> !task.isSucceeded()).toList();
    log.info("[DEBUG] not succeeded PDT size {}", notSucceededParcelDetectionTasks.size());
    if (succeededParcelDetectionTasks.size() == parcelDetectionTasks.size()) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "All parcel detection tasks of ZoneDetectionJob(id=" + jobId + ") are already SUCCEEDED");
    }
    var succeededJob =
        duplicateWithNewStatus(
            succeededJobId,
            job,
            succeededParcelDetectionTasks,
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
            notSucceededParcelDetectionTasks,
            JobStatus.builder()
                .jobId(succeededJobId)
                .progression(PENDING)
                .health(UNKNOWN)
                .creationDatetime(now())
                .build());

    detectionFilteredMailer.accept(new FilteredDetectionJob(jobId, succeededJob, notSucceededJob));
    log.info("Processing ZDJ(id={}) dispatch by success finished", jobId);
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
    log.info("[DEBUG] jobToDuplicate {}", jobToDuplicate);
    ZoneDetectionJob duplicatedJob = zdjRepository.save(jobToDuplicate);
    log.info("[DEBUG] saved duplicatedJob {}", duplicatedJob);
    processParcelDetectionDuplication(duplicatedJobId, tasks);
    return duplicatedJob;
  }

  private void processParcelDetectionDuplication(
      String duplicatedJobId, List<ParcelDetectionTask> tasks) {
    var detectionTaskDiffs =
        tasks.stream()
            .map(
                oldTask -> {
                  String newTaskId = randomUUID().toString();
                  String parcelId = randomUUID().toString();
                  String parcelContentId = randomUUID().toString();
                  String newAsJobId = randomUUID().toString();
                  var newTask =
                      oldTask.duplicate(
                          newTaskId, duplicatedJobId, parcelId, parcelContentId, newAsJobId);
                  log.info("[DEBUG] oldTask : {}, newTask {}", oldTask, newTask);
                  return new ParcelDetectionTask.ParcelDetectionTaskDiff(oldTask, newTask);
                })
            .toList();
    List<ParcelDetectionTask> newParcelDetectionTasks =
        parcelDetectionTaskRepository.saveAll(
            detectionTaskDiffs.stream()
                .map(ParcelDetectionTask.ParcelDetectionTaskDiff::newTask)
                .toList());
    log.info("[DEBUG] newParcelDetectionTasks.size {}", newParcelDetectionTasks.size());
    detectionTaskDiffs.forEach(
        diff -> {
          var newTask = diff.newTask();
          var oldTask = diff.oldTask();
          var oldTileDetectionTasks =
              tileDetectionTaskRepository.findAllByJobId(oldTask.getAsJobId());
          log.info("[DEBUG] oldTileDetectionTasks.size={}", oldTileDetectionTasks.size());
          var notSucceededTileTasks =
              oldTileDetectionTasks.stream()
                  .filter(tileDetectionTask -> !tileDetectionTask.isSucceeded())
                  .toList();
          log.info("[DEBUG] notSucceededTileTasks.size={}", notSucceededTileTasks.size());
          var succeededTileBucketPaths =
              oldTileDetectionTasks.stream()
                  .filter(Task::isSucceeded)
                  .map(task -> task.getTile().getBucketPath())
                  .toList();
          log.info("[DEBUG] succeededTileBucketPaths.size={}", succeededTileBucketPaths.size());
          var parcelDetectionJob = parcelJobConverter.apply(newTask);
          log.info("[DEBUG] duplicatedParcelDetectionJob={}", parcelDetectionJob);
          var newTileDetectionTasks =
              new ArrayList<>(
                  newTask.getTiles().stream()
                      .filter(keyPredicateFunction.apply(Tile::getBucketPath))
                      .map(
                          tile -> {
                            TileDetectionTask tileDetectionTask =
                                parcelJobConverter.apply(newTask, tile);
                            log.info("[DEBUG] duplicatedTileDetectionTask={}", tileDetectionTask);
                            return tileDetectionTask;
                          })
                      .toList());
          if (!notSucceededTileTasks.isEmpty()) {
            newTileDetectionTasks.removeIf(
                tileDetectionTask ->
                    succeededTileBucketPaths.contains(tileDetectionTask.getTile().getBucketPath()));
          } else {
            newTileDetectionTasks.forEach(
                tileDetectionTask ->
                    tileDetectionTask
                        .getStatusHistory()
                        .add(
                            TaskStatus.builder()
                                .id(randomUUID().toString())
                                .taskId(tileDetectionTask.getId())
                                .jobType(tileDetectionTask.getStatus().getJobType())
                                .progression(FINISHED)
                                .health(SUCCEEDED)
                                .creationDatetime(now())
                                .message(tileDetectionTask.getStatus().getMessage())
                                .build()));
            parcelDetectionJob
                .getStatusHistory()
                .add(
                    JobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(parcelDetectionJob.getId())
                        .jobType(parcelDetectionJob.getStatus().getJobType())
                        .progression(FINISHED)
                        .health(SUCCEEDED)
                        .creationDatetime(now())
                        .message(parcelDetectionJob.getStatus().getMessage())
                        .build());
          }
          ParcelDetectionJob savedPDJ =
              parcelDetectionJobService.save(parcelDetectionJob, newTileDetectionTasks);
          log.info(
              "[DEBUG] saved duplicatedPDJ={} with newTileDetectionTasks={}",
              savedPDJ,
              newTileDetectionTasks);
        });
  }
}
