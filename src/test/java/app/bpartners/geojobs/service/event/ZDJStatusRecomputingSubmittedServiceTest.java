package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.AnnotationRetrievingJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionInitiationService;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ZDJStatusRecomputingSubmittedServiceTest {
  ZoneDetectionJobService jobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationRetrievingJobService annotationRetrievingJobServiceMock = mock();
  GeoJsonConversionInitiationService geoJsonConversionInitiationServiceMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  JobAnnotationService jobAnnotationServiceMock = mock();
  ZDJStatusRecomputingSubmittedService subject =
      new ZDJStatusRecomputingSubmittedService(
          jobServiceMock,
          annotationRetrievingJobServiceMock,
          geoJsonConversionInitiationServiceMock,
          jobAnnotationServiceMock);

  @Disabled("TODO")
  @Test
  void accept_ok() {
    String jobId = "jobId";
    ZoneDetectionJob job = aZDJ(jobId, PROCESSING, UNKNOWN);
    when(jobServiceMock.findById(jobId)).thenReturn(job);
    when(jobServiceMock.recomputeStatus(job)).thenReturn(job);
    when(annotationRetrievingJobServiceMock.getByDetectionJobId(any())).thenReturn(List.of());

    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(job);
    assertDoesNotThrow(() -> subject.accept(new ZDJStatusRecomputingSubmitted(jobId)));

    verify(jobServiceMock, times(2)).findById(jobId);
    verify(jobServiceMock, times(1)).recomputeStatus(job);
    ArgumentCaptor<List<ZDJStatusRecomputingSubmitted>> listCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
  }

  private static ZoneDetectionJob aZDJ(
      String jobId, Status.ProgressionStatus progressionStatus, Status.HealthStatus healthStatus) {
    return ZoneDetectionJob.builder()
        .id(jobId)
        .zoneName("dummy")
        .emailReceiver("dummy")
        .detectionType(MACHINE)
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
