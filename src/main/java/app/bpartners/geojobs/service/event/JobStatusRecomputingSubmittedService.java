package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.JobStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.Job;
import app.bpartners.geojobs.job.model.Task;
import app.bpartners.geojobs.job.service.JobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class JobStatusRecomputingSubmittedService<
        J extends Job, T extends Task, E extends JobStatusRecomputingSubmitted>
    implements Consumer<E> {

  private JobService<T, J> jobService;

  @Override
  @SneakyThrows
  public void accept(E event) {
    var jobId = event.getJobId();
    var oldJob = jobService.findById(jobId);
    if (oldJob.isFinished()) {
      return;
    }

    jobService.recomputeStatus(oldJob);
    throw new RuntimeException("Fail on purpose so that message is not ack, causing retry");
  }
}
