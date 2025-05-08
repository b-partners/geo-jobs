package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.*;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobStatusChanged;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.job.model.Status.HealthStatus;
import app.bpartners.geojobs.job.model.Status.ProgressionStatus;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.TilingTaskRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.JobFinishedMailer;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.utils.tiling.TilingTaskCreator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@AutoConfigureMockMvc
class ZoneTilingJobStatusChangedServiceTest {
  JobFinishedMailer<ZoneTilingJob> mailerMock = mock();
  ZoneDetectionJobService jobServiceMock = mock();
  StatusChangedHandler statusChangedHandler = new StatusChangedHandler();
  DetectionRepository detectionRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  TilingTaskRepository tilingTaskRepositoryMock = mock();
  TilingTaskCreator tilingTaskCreator = new TilingTaskCreator();
  ZoneTilingJobStatusChangedService subject =
      new ZoneTilingJobStatusChangedService(
          mailerMock,
          jobServiceMock,
          statusChangedHandler,
          detectionRepositoryMock,
          eventProducerMock,
          objectConfigurationRepositoryMock,
          tilingTaskRepositoryMock);

  @BeforeEach
  void setUp() {
    when(tilingTaskRepositoryMock.findAllByJobId(any()))
        .thenReturn(
            List.of(
                tilingTaskCreator.create(
                    randomUUID().toString(),
                    randomUUID().toString(),
                    new Parcel(),
                    FINISHED,
                    SUCCEEDED)));
  }

  @Test
  void produces_failed_event_if_tiled_images_from_IGN() {
    reset(tilingTaskRepositoryMock);
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("priorityLayer", "FLUX_IGN_2023_20CM");
    var featureMock = mock(Feature.class);
    var parcel =
        Parcel.builder()
            .parcelContent(ParcelContent.builder().feature(featureMock).build())
            .build();
    when(featureMock.getProperties()).thenReturn(properties);
    when(tilingTaskRepositoryMock.findAllByJobId(any()))
        .thenReturn(
            List.of(
                tilingTaskCreator.create(
                    randomUUID().toString(),
                    randomUUID().toString(),
                    parcel,
                    FINISHED,
                    SUCCEEDED)));

    var oldJob = aZTJ(PROCESSING, UNKNOWN);
    var newJob = aZTJ(FINISHED, SUCCEEDED);

    subject.accept(new ZoneTilingJobStatusChanged(oldJob, newJob));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(jobServiceMock, never()).saveZDJFromZTJ(any());
    verify(detectionRepositoryMock, never()).findByEndToEndIdAndCommunityOwnerId(any(), any());
    verify(mailerMock, never()).accept(any());
    verify(eventProducerMock, only()).accept(listCaptor.capture());
    var failedEvent = (ZoneTilingJobFailed) listCaptor.getValue().getFirst();
    assertEquals(new ZoneTilingJobFailed(newJob), failedEvent);
  }

  @Test
  void do_not_mail_if_old_fails_and_new_fails() {
    when(jobServiceMock.saveZDJFromZTJ(any()))
        .thenReturn(ZoneDetectionJob.builder().id("zdj_id").build());
    when(detectionRepositoryMock.findByEndToEndIdAndCommunityOwnerId(any(), any()))
        .thenReturn(Optional.ofNullable(Detection.builder().build()));
    var ztjStatusChanged = new ZoneTilingJobStatusChanged();
    ztjStatusChanged.setOldJob(aZTJ(FINISHED, FAILED));
    ztjStatusChanged.setNewJob(aZTJ(FINISHED, FAILED));

    subject.accept(ztjStatusChanged);

    verify(mailerMock, times(0)).accept(any());
  }

  @Test
  void mail_if_old_unknown_and_new_fails() {
    var ztjStatusChanged = new ZoneTilingJobStatusChanged();
    var oldJob = aZTJ(PROCESSING, UNKNOWN);
    var newJob = aZTJ(FINISHED, FAILED);
    ztjStatusChanged.setOldJob(oldJob);
    ztjStatusChanged.setNewJob(newJob);

    subject.accept(ztjStatusChanged);

    var eventListCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, only()).accept(eventListCaptor.capture());
    var zoneTilingJobFailed = (ZoneTilingJobFailed) eventListCaptor.getValue().getFirst();
    assertEquals(ZoneTilingJobFailed.builder().failedJob(newJob).build(), zoneTilingJobFailed);
  }

  @Test
  void do_nothing() {
    var ztjStatusChanged1 = new ZoneTilingJobStatusChanged();
    var ztjStatusChanged2 = new ZoneTilingJobStatusChanged();
    ztjStatusChanged1.setOldJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged1.setNewJob(aZTJ(PROCESSING, UNKNOWN));
    ztjStatusChanged2.setOldJob(aZTJ(PENDING, UNKNOWN));
    ztjStatusChanged2.setNewJob(aZTJ(PROCESSING, UNKNOWN));

    subject.accept(ztjStatusChanged1);
    subject.accept(ztjStatusChanged2);

    verify(jobServiceMock, times(0)).saveZDJFromZTJ(any());
    verify(mailerMock, times(0)).accept(any());
  }

  private static ZoneTilingJob aZTJ(ProgressionStatus progression, HealthStatus health) {
    var statusHistory = new ArrayList<JobStatus>();
    statusHistory.add(JobStatus.builder().progression(progression).health(health).build());
    return ZoneTilingJob.builder().statusHistory(statusHistory).build();
  }
}
