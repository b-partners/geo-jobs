package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZoneDetectionJobFailedServiceTest {
  DetectionFinishedMailer mailerMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ZoneDetectionJobFailedService subject =
      new ZoneDetectionJobFailedService(
          mailerMock, zoneDetectionJobServiceMock, detectionRepositoryMock);

  @Test
  void send_email_with_detection_e2Id() {
    var jobId = randomUUID().toString();
    var detectionE2Id = randomUUID().toString();
    var emailReceiver = "emailReceiver";
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    var detectionMock = mock(Detection.class);
    when(zoneDetectionJobMock.getId()).thenReturn(jobId);
    when(zoneDetectionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(zoneDetectionJobMock);
    when(detectionRepositoryMock.findByZdjId(jobId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobFailed(jobId)));

    verify(mailerMock, only())
        .accept(
            emailReceiver,
            "Erreur lors du traitement de la détection portant l'ID " + detectionE2Id);
  }

  @Test
  void send_email_with_zdj_id() {
    var jobId = randomUUID().toString();
    var emailReceiver = "emailReceiver";
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(zoneDetectionJobMock.getId()).thenReturn(jobId);
    when(zoneDetectionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(zoneDetectionJobServiceMock.findById(jobId)).thenReturn(zoneDetectionJobMock);
    when(detectionRepositoryMock.findByZdjId(jobId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobFailed(jobId)));

    verify(mailerMock, only())
        .accept(
            emailReceiver, "Erreur lors du traitement de la détection machine (ZDJ=" + jobId + ")");
  }
}
