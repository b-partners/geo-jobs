package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import org.junit.jupiter.api.Test;

class ZoneDetectionJobFailedServiceTest {
  ZoneDetectionJobFailedService subject = new ZoneDetectionJobFailedService();

  @Test
  void accept_ko() {
    assertThrows(NotImplementedException.class, () -> subject.accept(new ZoneDetectionJobFailed()));
  }
}
