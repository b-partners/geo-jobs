package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class ZoneTilingJobStatusChangedServiceTest {
  JobFinishedMailer<ZoneTilingJob> mailer = mock();
  ZoneDetectionJobService jobService = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  DetectionRepository detectionRepository = mock();
  EventProducer eventProducerMock = mock();
  ZoneTilingJobStatusChangedService subject =
      new ZoneTilingJobStatusChangedService(
          mailer, jobService, statusChangedHandler, detectionRepository, eventProducerMock);

  @Test
  void do_not_mail_if_old_fails_and_new_fails() {
    when(jobService.saveZDJFromZTJ(any()))
        .thenReturn(ZoneDetectionJob.builder().id("zdj_id").build());
    when(detectionRepository.findByEndToEndId(any()))
        .thenReturn(Optional.ofNullable(Detection.builder().build()));
    var ztjStatusChanged = new ZoneTilingJobStatusChanged();
    ztjStatusChanged.setOldJob(aZTJ(FINISHED, FAILED));
    ztjStatusChanged.setNewJob(aZTJ(FINISHED, FAILED));

    subject.accept(ztjStatusChanged);

    verify(mailer, times(0)).accept(any());
  }

  @Test
  void mail_if_old_unknown_and_new_fails() {
    var ztjStatusChanged = new ZoneTilingJobStatusChanged();
    ztjStatusChanged.setOldJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged.setNewJob(aZTJ(FINISHED, FAILED));
    when(detectionRepository.findByEndToEndId(any()))
        .thenReturn(
            Optional.ofNullable(
                Detection.builder()
                    .endToEndId("end_to_end_id")
                    .ztjId("ztj_id")
                    .zdjId("zdj_id")
                    .build()));
    when(jobService.saveZDJFromZTJ(any())).thenReturn(ZoneDetectionJob.builder().build());
    subject.accept(ztjStatusChanged);

    verify(mailer, times(1)).accept(any());
  }

  @Test
  void do_nothing() {
    var ztjStatusChanged1 = new ZoneTilingJobStatusChanged();
    var ztjStatusChanged2 = new ZoneTilingJobStatusChanged();
    ztjStatusChanged1.setOldJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged1.setNewJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged2.setOldJob(aZTJ(PENDING, UNKNOWN));
    ztjStatusChanged2.setNewJob(aZTJ(PROCESSING, UNKNOWN));

    subject.accept(ztjStatusChanged1);
    subject.accept(ztjStatusChanged2);

    verify(jobService, times(0)).saveZDJFromZTJ(any());
    verify(mailer, times(0)).accept(any());
  }

  private static ZoneTilingJob aZTJ(ProgressionStatus progression, HealthStatus health) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(JobStatus.builder().progression(progression).health(health).build());
    return ZoneTilingJob.builder().statusHistory(statusHistory).build();
  }
}
