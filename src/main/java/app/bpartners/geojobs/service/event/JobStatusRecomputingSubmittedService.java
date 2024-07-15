package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.JobStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class JobStatusRecomputingSubmittedService<
        J extends Job, T extends Task, E extends JobStatusRecomputingSubmitted>
    implements Consumer<E> {
  private static final int ATTEMPT_FOR_256_MINUTES_DURATION = 6;
  private final EventProducer eventProducer;
  private JobService<T, J> jobService;
  private final TaskStatusService<T> taskStatusService;
  private final TaskRepository<T> taskRepository;

  @Override
  @SneakyThrows
  public void accept(E event) {
    var jobId = event.getJobId();
    var lastDurationValue = event.getMaxConsumerBackoffBetweenRetriesDurationValue();
    var attemptNb = event.getAttemptNb();
    var oldJob = jobService.findById(jobId);
    var newJob = jobService.recomputeStatus(oldJob);

    if (!(newJob.isFailed() || newJob.isSucceeded())) {
      Class<? extends JobStatusRecomputingSubmitted> clazz = event.getClass();
      if (attemptNb < ATTEMPT_FOR_256_MINUTES_DURATION) {
        long maxConsumerBackoffBetweenRetriesDurationValue = lastDurationValue * 2;
        int newAttemptNb = attemptNb + 1;
        var newEvent =
            clazz
                .getDeclaredConstructor(String.class, Long.class, Integer.class)
                .newInstance(
                    newJob.getId(), maxConsumerBackoffBetweenRetriesDurationValue, newAttemptNb);
        newEvent.setJobId(newJob.getId());
        newEvent.setMaxConsumerDurationValue(maxConsumerBackoffBetweenRetriesDurationValue);
        newEvent.setAttemptNb(attemptNb);
        eventProducer.accept(List.of(newEvent));
      } else {
        log.error("Max attempt reached for " + clazz.getSimpleName() + " handler");
        var tasks = taskRepository.findAllByJobId(jobId);
        var notFinishedTasks = tasks.stream().filter(task -> !task.isFinished()).toList();
        notFinishedTasks.forEach(taskStatusService::fail);
        jobService.recomputeStatus(newJob);
      }
    } else {
      log.error(
          "Nothing happens while oldJobStatus={} and newJobStatus={} for job.id={}",
          oldJob.getStatus(),
          newJob.getStatus(),
          jobId);
    }
  }
}
