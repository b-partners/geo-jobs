package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.ZoneTask;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneTaskStatusService<T extends ZoneTask> {

  public T pending(T task, Function<T, T> update) {
    return updateStatus(task, PENDING, UNKNOWN, update);
  }

  public T process(T task, Function<T, T> update) {
    return updateStatus(task, PROCESSING, UNKNOWN, update);
  }

  public T succeed(T task, Function<T, T> update) {
    return updateStatus(task, FINISHED, SUCCEEDED, update);
  }

  public T fail(T task, Function<T, T> update) {
    return updateStatus(task, FINISHED, FAILED, update);
  }

  private T updateStatus(
      T task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      Function<T, T> update) {
    task.addStatus(
        TaskStatus.builder()
            .id(randomUUID().toString())
            .creationDatetime(now())
            .progression(progression)
            .health(health)
            .taskId(task.getId())
            .build());
    return update.apply(task);
  }
}
