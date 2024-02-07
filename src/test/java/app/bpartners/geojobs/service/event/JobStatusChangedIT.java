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
import app.bpartners.geojobs.repository.model.JobStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.Parcel;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geo.tiling.TilingFinishedMailer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class JobStatusChangedIT extends FacadeIT {
  @Autowired private ZoneTilingJobStatusChangedService subject;
  @MockBean private TilingFinishedMailer mailer;
  @MockBean private ZoneDetectionJobService zdjService;

  @Test
  void send_email_ok() {
    var jobId = randomUUID().toString();
    var taskId = randomUUID().toString();
    var newZtj =
        ZoneTilingJob.builder()
            .id(jobId)
            .zoneName("mock")
            .emailReceiver("mock@gmail.com")
            .tasks(
                List.of(
                    TilingTask.builder()
                        .id(taskId)
                        .jobId(jobId)
                        .submissionInstant(now())
                        .parcel(Parcel.builder().id(randomUUID().toString()).build())
                        .statusHistory(
                            List.of(
                                TaskStatus.builder()
                                    .id(randomUUID().toString())
                                    .progression(FINISHED)
                                    .health(SUCCEEDED)
                                    .taskId(taskId)
                                    .creationDatetime(now())
                                    .build()))
                        .build()))
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .id(randomUUID().toString())
                        .jobId(jobId)
                        .progression(FINISHED)
                        .health(SUCCEEDED)
                        .build()))
            .build();
    var zoneTilingJobStatusChanged =
        ZoneTilingJobStatusChanged.builder().oldJob(new ZoneTilingJob()).newJob(newZtj).build();

    subject.accept(zoneTilingJobStatusChanged);

    verify(zdjService, times(1)).saveZDJFromZTJ(zoneTilingJobStatusChanged.getNewJob());
    verify(mailer, times(1)).accept(any());
  }
}
