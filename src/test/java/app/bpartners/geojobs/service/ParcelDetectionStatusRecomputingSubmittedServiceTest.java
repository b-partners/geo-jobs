package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.detection.ParcelDetectionJobService;
import app.bpartners.geojobs.service.event.ParcelDetectionStatusRecomputingSubmittedService;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ParcelDetectionStatusRecomputingSubmittedServiceTest {
  private static final String JOB_ID = "jobId";
  ParcelDetectionJobService parcelDetectionJobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  ParcelDetectionStatusRecomputingSubmittedService subject =
      new ParcelDetectionStatusRecomputingSubmittedService(parcelDetectionJobServiceMock);

  @Disabled("TODO")
  @Test
  void accept_ok() {
    ParcelDetectionJob parcelDetectionJob = aPDJ(JOB_ID, PROCESSING, UNKNOWN);
    when(parcelDetectionJobServiceMock.findById(JOB_ID)).thenReturn(parcelDetectionJob);
    when(parcelDetectionJobServiceMock.recomputeStatus(parcelDetectionJob))
        .thenReturn(parcelDetectionJob);

    assertDoesNotThrow(() -> subject.accept(new ParcelDetectionStatusRecomputingSubmitted(JOB_ID)));

    verify(parcelDetectionJobServiceMock, times(1)).findById(JOB_ID);
    verify(parcelDetectionJobServiceMock, times(1)).recomputeStatus(parcelDetectionJob);
    ArgumentCaptor<List<ParcelDetectionStatusRecomputingSubmitted>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
  }

  private static ParcelDetectionJob aPDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ParcelDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .statusHistory(
            List.of(
                JobStatus.builder()
                    .id(randomUUID().toString())
                    .jobId(jobId)
                    .progression(progressionStatus)
                    .health(healthStatus)
                    .build()))
        .build();
  }
}
