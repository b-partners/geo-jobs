package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.AutoTaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.TaskStatisticRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.TaskStatisticRecomputingSubmittedConsumer;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class AutoTaskStatisticRecomputingSubmittedService
    implements Consumer<AutoTaskStatisticRecomputingSubmitted> {
  private static final int ATTEMPT_FOR_256_MINUTES_DURATION = 6;
  private final EventProducer eventProducer;
  private final TaskStatisticRecomputingSubmittedConsumer<TilingTask, ZoneTilingJob>
      tilingStatisticConsumer;
  private final TaskStatisticRecomputingSubmittedConsumer<ParcelDetectionTask, ZoneDetectionJob>
      zoneDetectionStatisticConsumer;
  private final ZoneTilingJobRepository tilingJobRepository;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;

  public AutoTaskStatisticRecomputingSubmittedService(
      EventProducer eventProducer,
      ZoneDetectionJobRepository zoneDetectionJobRepository,
      ParcelDetectionTaskRepository parcelDetectionTaskRepository,
      ZoneTilingJobRepository zoneTilingJobRepository,
      TilingTaskRepository tilingTaskRepository,
      TaskStatisticRepository taskStatisticRepository) {
    this.eventProducer = eventProducer;
    this.tilingJobRepository = zoneTilingJobRepository;
    this.zoneDetectionJobRepository = zoneDetectionJobRepository;
    this.zoneDetectionStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneDetectionJobRepository, parcelDetectionTaskRepository, taskStatisticRepository);
    this.tilingStatisticConsumer =
        new TaskStatisticRecomputingSubmittedConsumer<>(
            zoneTilingJobRepository, tilingTaskRepository, taskStatisticRepository);
  }

  @Override
  public void accept(AutoTaskStatisticRecomputingSubmitted event) {
    var jobId = event.getJobId();
    var lastDurationValue = event.getMaxConsumerBackoffBetweenRetriesDurationValue();
    var attemptNb = event.getAttemptNb();
    var optionalTiling = tilingJobRepository.findById(jobId);
    if (optionalTiling.isEmpty()) {
      var zoneDetectionJob =
          zoneDetectionJobRepository
              .findById(jobId)
              .orElseThrow(() -> new NotFoundException("Job.id=" + jobId + " not found"));
      processJob(zoneDetectionJob, jobId, lastDurationValue, attemptNb);
    } else {
      var tilingJob = optionalTiling.get();
      processJob(tilingJob, jobId, lastDurationValue, attemptNb);
    }
  }

  private void processJob(Job job, String jobId, long lastDurationValue, int attemptNb) {
    var taskStatisticRecomputingSubmitted = new TaskStatisticRecomputingSubmitted(jobId);
    if (job instanceof ZoneDetectionJob) {
      zoneDetectionStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    } else if (job instanceof ZoneTilingJob) {
      tilingStatisticConsumer.accept(taskStatisticRecomputingSubmitted);
    }

    if (!job.isFailed() && !job.isSucceeded()) {
      if (attemptNb < ATTEMPT_FOR_256_MINUTES_DURATION) {
        long maxConsumerBackoffBetweenRetriesDurationValue = lastDurationValue * 2;
        int newAttemptNb = attemptNb + 1;
        eventProducer.accept(
            List.of(
                new AutoTaskStatisticRecomputingSubmitted(
                    jobId, maxConsumerBackoffBetweenRetriesDurationValue, newAttemptNb)));
      }
    }
  }
}
