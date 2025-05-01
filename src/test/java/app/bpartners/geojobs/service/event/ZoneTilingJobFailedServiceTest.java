package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ZoneTilingJobFailedServiceTest {
  DetectionFinishedMailer detectionFinishedMailerMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  ZoneTilingJobFailedService subject =
      new ZoneTilingJobFailedService(detectionFinishedMailerMock, detectionRepositoryMock);

  @Test
  void trigger_email_containing_detection_id() {
    var jobId = randomUUID().toString();
    var detectionE2Id = randomUUID().toString();
    var emailReceiver = "dummy@email.com";
    var zoneTilingJob = mock(ZoneTilingJob.class);
    var detectionMock = mock(Detection.class);
    when(zoneTilingJob.getId()).thenReturn(jobId);
    when(zoneTilingJob.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(detectionRepositoryMock.findByZtjId(jobId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(() -> subject.accept(new ZoneTilingJobFailed(zoneTilingJob)));

    verify(detectionFinishedMailerMock, only())
        .accept(
            eq(emailReceiver),
            eq("Erreur survenue lors du traitement de la détection portant l'ID " + detectionE2Id));
  }

  @Test
  void trigger_email_containing_tiling_job_id() {
    var jobId = randomUUID().toString();
    var emailReceiver = "dummy@email.com";
    var zoneTilingJob = mock(ZoneTilingJob.class);
    when(zoneTilingJob.getId()).thenReturn(jobId);
    when(zoneTilingJob.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionRepositoryMock.findByZtjId(jobId)).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(new ZoneTilingJobFailed(zoneTilingJob)));

    verify(detectionFinishedMailerMock, only())
        .accept(
            eq(emailReceiver),
            eq("Erreur survenue lors du traitement du pavage (ZDJ=" + jobId + ")"));
  }
}
