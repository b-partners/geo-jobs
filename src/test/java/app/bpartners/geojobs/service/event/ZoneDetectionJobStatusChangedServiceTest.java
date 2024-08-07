package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class ZoneDetectionJobStatusChangedServiceTest {
  JobFinishedMailer<ZoneDetectionJob> detectionFinishedMailerMock = mock();
  EventProducer eventProducerMock = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  ZoneDetectionJobStatusChangedService subject =
      new ZoneDetectionJobStatusChangedService(
          detectionFinishedMailerMock, eventProducerMock, statusChangedHandler);

  @Test
  void do_not_mail_if_old_fails_and_new_fails() {
    var ztjStatusChanged = new ZoneDetectionJobStatusChanged();
    ztjStatusChanged.setOldJob(aZDJ(FINISHED, FAILED));
    ztjStatusChanged.setNewJob(aZDJ(FINISHED, FAILED));

    subject.accept(ztjStatusChanged);

    verify(detectionFinishedMailerMock, times(0)).accept(any());
  }

  @Test
  void do_not_mail_if_old_unknown_and_new_fails() {
    var ztjStatusChanged = new ZoneDetectionJobStatusChanged();
    ztjStatusChanged.setOldJob(aZDJ(PROCESSING, UNKNOWN));
    ztjStatusChanged.setNewJob(aZDJ(FINISHED, FAILED));

    assertThrows(ApiException.class, () -> subject.accept(ztjStatusChanged));

    verify(detectionFinishedMailerMock, times(0)).accept(any());
  }

  @Test
  void do_nothing_if_old_equals_new() {
    var ztjStatusChanged = new ZoneDetectionJobStatusChanged();
    ztjStatusChanged.setOldJob(aZDJ(PROCESSING, UNKNOWN));
    ztjStatusChanged.setNewJob(aZDJ(PROCESSING, UNKNOWN));

    subject.accept(ztjStatusChanged);

    verify(eventProducerMock, times(0)).accept(any());
    verify(detectionFinishedMailerMock, times(0)).accept(any());
  }

  private static ZoneDetectionJob aZDJ(
      Status.ProgressionStatus progression, Status.HealthStatus health) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(JobStatus.builder().progression(progression).health(health).build());
    return ZoneDetectionJob.builder().statusHistory(statusHistory).build();
  }
}
