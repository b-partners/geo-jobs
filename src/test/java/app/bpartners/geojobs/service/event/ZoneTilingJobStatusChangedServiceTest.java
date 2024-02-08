package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.Status.HealthStatus.FAILED;
import static app.bpartners.geojobs.repository.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.Status.ProgressionStatus.PROCESSING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.endpoint.event.gen.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.repository.model.Status.HealthStatus;
import app.bpartners.geojobs.repository.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.model.TaskStatus;
import app.bpartners.geojobs.repository.model.geo.tiling.TilingTask;
import app.bpartners.geojobs.repository.model.geo.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.geo.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geo.tiling.TilingFinishedMailer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneTilingJobStatusChangedServiceTest {

  TilingFinishedMailer mailer = mock();
  ZoneDetectionJobService jobService = mock();
  ZoneTilingJobStatusChangedService subject =
      new ZoneTilingJobStatusChangedService(mailer, jobService);

  @Test
  void do_not_mail_if_old_fails_and_new_fails() {
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

    subject.accept(ztjStatusChanged);

    verify(mailer, times(1)).accept(any());
  }

  @Test
  void do_nothing_if_old_equals_new() {
    var ztjStatusChanged = new ZoneTilingJobStatusChanged();
    ztjStatusChanged.setOldJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged.setNewJob(aZTJ(PROCESSING, UNKNOWN));

    subject.accept(ztjStatusChanged);

    verify(jobService, times(0)).saveZDJFromZTJ(any());
    verify(mailer, times(0)).accept(any());
  }

  private static ZoneTilingJob aZTJ(ProgressionStatus progression, HealthStatus health) {
    return ZoneTilingJob.builder()
        .tasks(
            List.of(
                TilingTask.builder()
                    .statusHistory(
                        List.of(
                            TaskStatus.builder().progression(progression).health(health).build()))
                    .build()))
        .build();
  }
}
