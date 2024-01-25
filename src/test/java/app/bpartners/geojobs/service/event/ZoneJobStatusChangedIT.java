package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.TilingJobStatus;
import app.bpartners.geojobs.repository.model.TilingTaskStatus;
import app.bpartners.geojobs.repository.model.ZoneTilingJob;
import app.bpartners.geojobs.repository.model.ZoneTilingTask;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import app.bpartners.geojobs.service.EmailService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ZoneTilingJobStatusChangedIT extends FacadeIT {
  @Autowired private ZoneTilingJobStatusChangedService subject;
  @MockBean private EmailService emailService;

  @Test
  void send_email_ok() {
    String jobId = randomUUID().toString();
    String taskId = randomUUID().toString();
    ZoneTilingJob toCreate =
        ZoneTilingJob.builder()
            .id(jobId)
            .zoneName("mock")
            .emailReceiver("mock@gmail.com")
            .tasks(
                List.of(
                    ZoneTilingTask.builder()
                        .id(taskId)
                        .jobId(jobId)
                        .submissionInstant(now())
                        .parcel(Parcel.builder().id(randomUUID().toString()).build())
                        .statusHistory(
                            List.of(
                                TilingTaskStatus.builder()
                                    .id(randomUUID().toString())
                                    .progression(FINISHED)
                                    .health(SUCCEEDED)
                                    .taskId(taskId)
                                    .creationDatetime(now())
                                    .build()))
                        .build()))
            .statusHistory(
                List.of(
                    TilingJobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(jobId)
                        .progression(FINISHED)
                        .health(SUCCEEDED)
                        .build()))
            .build();
    ZoneTilingJobStatusChanged zoneTilingJobStatusChanged =
        ZoneTilingJobStatusChanged.builder().newJob(toCreate).build();

    subject.accept(zoneTilingJobStatusChanged);

    verify(emailService, times(1)).sendEmail(any());
  }
}
