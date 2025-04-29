package app.bpartners.geojobs.endpoint.event.model;

import app.bpartners.geojobs.job.model.Task;
import java.time.Duration;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@ToString
public class TaskCreated<T extends Task> extends PojaEvent {

  protected T task;

  protected TaskCreated(T task) {
    this.task = task;
  }

  @Override
  public Duration maxConsumerDuration() {
    return null;
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return null;
  }
}
