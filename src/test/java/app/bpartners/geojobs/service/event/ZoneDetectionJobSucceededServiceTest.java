package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import org.junit.jupiter.api.Test;

class ZoneDetectionJobSucceededServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  ZoneDetectionFinishedConsumer finishedConsumerMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(finishedConsumerMock);

  @Test
  void accept_ok() {
    subject.accept(ZoneDetectionJobSucceeded.builder().succeededJobId(MOCK_JOB_ID).build());

    verify(finishedConsumerMock, times(1)).accept(MOCK_JOB_ID);
  }
}
