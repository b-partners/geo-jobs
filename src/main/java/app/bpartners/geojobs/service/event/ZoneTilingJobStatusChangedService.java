package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.ZoneTilingJob;
import app.bpartners.geojobs.service.EmailService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ZoneTilingJobStatusChangedService implements Consumer<ZoneTilingJobStatusChanged> {

  private EmailService emailService;

  @Override
  public void accept(ZoneTilingJobStatusChanged zoneTilingJobStatusChanged) {
    ZoneTilingJob newJob = zoneTilingJobStatusChanged.getNewJob();

    JobStatus status = newJob.getStatus();

    if (Status.ProgressionStatus.FINISHED.equals(status.getProgression())
        && Status.HealthStatus.SUCCEEDED.equals(status.getHealth())) {
      emailService.sendEmail(newJob);
    }
  }
}
