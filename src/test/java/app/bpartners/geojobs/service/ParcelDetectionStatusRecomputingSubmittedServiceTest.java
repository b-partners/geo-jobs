package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.event.ParcelDetectionStatusRecomputingSubmittedService;
import org.junit.jupiter.api.Test;

public class ParcelDetectionStatusRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  ParcelDetectionJobService parcelDetectionJobServiceMock = mock();
  ParcelDetectionStatusRecomputingSubmittedService subject =
      new ParcelDetectionStatusRecomputingSubmittedService(parcelDetectionJobServiceMock);

  @Test
  void accept_ok() {
    ParcelDetectionJob parcelDetectionJob = new ParcelDetectionJob();
    when(parcelDetectionJobServiceMock.findById(JOB_ID)).thenReturn(parcelDetectionJob);

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionStatusRecomputingSubmitted(JOB_ID)));

    verify(parcelDetectionJobServiceMock, times(1)).findById(JOB_ID);
    verify(parcelDetectionJobServiceMock, times(1)).recomputeStatus(parcelDetectionJob);
  }
}
