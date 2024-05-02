package app.bpartners.geojobs.job.service;

import static java.lang.Thread.sleep;

import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import java.time.Duration;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class RetryableTaskToTaskStatusService<
    T_CHILD extends Task, T_PARENT extends Task, J extends Job> {

  private final TaskToTaskStatusService<T_CHILD, T_PARENT, J> taskStatusService;
  private final Duration maxSleepDuration;
  private final int maxRetry;

  public T_CHILD process(T_CHILD task) {
    return retry(taskStatusService::process, task, 0);
  }

  public T_CHILD succeed(T_CHILD task) {
    return retry(taskStatusService::succeed, task, 0);
  }

  public T_CHILD fail(T_CHILD task) {
    return retry(taskStatusService::fail, task, 0);
  }

  @SneakyThrows
  private T_CHILD retry(Function<T_CHILD, T_CHILD> taskFunction, T_CHILD task, int attemptNb) {
    try {
      return taskFunction.apply(task);
    } catch (Exception e) {
      if (attemptNb >= maxRetry) {
        throw e;
      }
      var sleepDuration = (long) (maxSleepDuration.toMillis() * Math.random());
      log.info("Retry: attemptNb={}/{}, sleep(ms)={}", attemptNb, maxRetry, sleepDuration, e);
      sleep(sleepDuration);
      return retry(taskFunction, task, attemptNb + 1);
    }
  }
}
