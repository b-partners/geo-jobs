package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.ZoneTilingJobStatusChangedService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneTilingJobStatusChangedServiceTest {
  FullDetectionRepository fullDetectionRepository = mock();
  ZoneDetectionJobService zoneDetectionJobService = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  EventProducer eventProducerMock = mock();
  ZoneTilingJobStatusChangedService subject =
      new ZoneTilingJobStatusChangedService(
          mock(),
          zoneDetectionJobService,
          statusChangedHandler,
          fullDetectionRepository,
          eventProducerMock);

  @Test
  void should_save_fullDetection_if_its_not_null() {
    ZoneDetectionJob zdj = mock();
    FullDetection fullDetection = mock();
    ZoneTilingJob oldJob = mock();
    ZoneTilingJob newJob = mock();
    var newStatus =
        JobStatus.builder().progression(FINISHED).health(Status.HealthStatus.SUCCEEDED).build();
    var oldStatus =
        JobStatus.builder().progression(PROCESSING).health(Status.HealthStatus.SUCCEEDED).build();

    var zoneTilingJobStatusChanged = new ZoneTilingJobStatusChanged(oldJob, newJob);

    when(zoneDetectionJobService.saveZDJFromZTJ(any())).thenReturn(zdj);
    when(fullDetectionRepository.findByZtjId(any())).thenReturn(Optional.of(fullDetection));
    when(oldJob.getStatus()).thenReturn(oldStatus);
    when(newJob.getStatus()).thenReturn(newStatus);

    subject.accept(zoneTilingJobStatusChanged);

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    verify(fullDetection, times(1)).setZdjId(any());
    verify(fullDetectionRepository, times(1)).save(any());
    var event = ((List<ZoneDetectionJobCreated>) listCaptor.getValue()).getFirst();
    assertEquals(ZoneDetectionJobCreated.builder().zoneDetectionJob(zdj).build(), event);
  }
}
