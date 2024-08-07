package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.ZoneTilingJobStatusChangedService;
import org.junit.jupiter.api.Test;

class ZoneTilingJobStatusChangedServiceTest {
  FullDetectionRepository fullDetectionRepository = mock();
  ZoneDetectionJobService zoneDetectionJobService = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  ZoneTilingJobStatusChangedService subject =
      new ZoneTilingJobStatusChangedService(
          mock(), zoneDetectionJobService, statusChangedHandler, fullDetectionRepository);

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
    when(fullDetectionRepository.findByZtjId(any())).thenReturn(fullDetection);
    when(oldJob.getStatus()).thenReturn(oldStatus);
    when(newJob.getStatus()).thenReturn(newStatus);

    subject.accept(zoneTilingJobStatusChanged);

    verify(fullDetection, times(1)).setZdjId(any());
    verify(fullDetectionRepository, times(1)).save(any());
  }
}
