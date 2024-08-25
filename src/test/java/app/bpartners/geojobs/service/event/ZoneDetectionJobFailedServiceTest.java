package app.bpartners.geojobs.service.event;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import org.junit.jupiter.api.Test;

class ZoneDetectionJobFailedServiceTest {
  private static final String DETECTION_JOB_ID = "detectionJobId";
  ZoneDetectionFinishedConsumer finishedConsumerMock = mock();
  ZoneDetectionJobService jobServiceMock = mock();
  ZoneDetectionJobFailedService subject =
      new ZoneDetectionJobFailedService(finishedConsumerMock, jobServiceMock);

  @Test
  void process_ok() {
    when(jobServiceMock.findById(DETECTION_JOB_ID)).thenReturn(new ZoneDetectionJob());

    subject.accept(ZoneDetectionJobFailed.builder().failedJobId(DETECTION_JOB_ID).build());

    verify(finishedConsumerMock, times(1)).accept(DETECTION_JOB_ID);
  }
}
