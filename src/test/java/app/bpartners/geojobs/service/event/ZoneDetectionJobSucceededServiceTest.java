package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.model.detection.*;
import org.junit.jupiter.api.Test;

public class ZoneDetectionJobSucceededServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  ZoneDetectionJobAnnotationProcessor jobAnnotationProcessorMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(jobAnnotationProcessorMock);

  @Test
  void accept_ok() {
    subject.accept(ZoneDetectionJobSucceeded.builder().succeededJobId(MOCK_JOB_ID).build());

    verify(jobAnnotationProcessorMock, times(1)).accept(any());
  }
}
