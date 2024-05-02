package app.bpartners.geojobs.job;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.JobType;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.service.RetryableTaskToTaskStatusService;
import app.bpartners.geojobs.job.service.TaskToTaskStatusService;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryableTaskToTaskStatusServiceTest {
  TaskToTaskStatusService service = mock();
  RetryableTaskToTaskStatusService subject;

  {
    subject = new RetryableTaskToTaskStatusService(service, Duration.of(5, SECONDS), 3);
  }

  @Test
  void do_all_retries() {
    var task = aTask();
    when(service.process(task)).thenThrow(new RuntimeException());

    assertThrows(RuntimeException.class, () -> subject.process(task));

    verify(service, times(1 /*try*/ + 3 /*retry*/)).process(task);
  }

  private static Task aTask() {
    return new Task() {
      @Override
      public JobType getJobType() {
        return null;
      }

      @Override
      public Task semanticClone() {
        return null;
      }
    };
  }
}
