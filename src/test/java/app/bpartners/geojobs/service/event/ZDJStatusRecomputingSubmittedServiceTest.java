package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PROCESSING;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.job.repository.TaskRepository;
import app.bpartners.geojobs.job.service.JobAnnotationService;
import app.bpartners.geojobs.job.service.TaskStatusService;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ZDJStatusRecomputingSubmittedServiceTest {
  ZoneDetectionJobService jobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  TaskStatusService<ParcelDetectionTask> taskStatusServiceMock = mock();
  TaskRepository<ParcelDetectionTask> taskRepositoryMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  JobAnnotationService jobAnnotationServiceMock = mock();
  ZDJStatusRecomputingSubmittedService subject =
      new ZDJStatusRecomputingSubmittedService(
          jobServiceMock,
          eventProducerMock,
          taskStatusServiceMock,
          taskRepositoryMock,
          zoneDetectionJobServiceMock,
          jobAnnotationServiceMock);

  @Test
  void accept_ok() {
    String jobId = "jobId";
    ZoneDetectionJob job = aZDJ(jobId, PROCESSING, UNKNOWN);
    when(jobServiceMock.findById(jobId)).thenReturn(job);
    when(jobServiceMock.recomputeStatus(job)).thenReturn(job);
    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(job);
    assertDoesNotThrow(() -> subject.accept(new ZDJStatusRecomputingSubmitted(jobId)));

    verify(jobServiceMock, times(1)).findById(jobId);
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
