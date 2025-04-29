package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.event.GeoJsonConversionAssemblySucceededService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

class GeoJsonConversionAssemblySucceededServiceTest {
  DetectionFinishedMailer detectionFinishedMailerMock = mock();
  GeoJsonConversionAssemblySucceededService subject =
      new GeoJsonConversionAssemblySucceededService(detectionFinishedMailerMock);

  @Test
  void trigger_detection_finished_mailer_from_geo_json_conversion_succeeded() {
    var succeededGeoJsonConversionJobMock = mock(GeoJsonConversionJob.class);
    var jobStatusMock = mock(JobStatus.class);
    var creationDatetime = now();
    var emailReceiver = "emailReceiver";
    var zoneName = "zoneName";
    var emailSubject =
        String.format(
            "Analyse sur la zone %s terminée le %s",
            zoneName,
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .format(creationDatetime.atZone(ZoneId.of("Europe/Paris"))));
    when(jobStatusMock.getCreationDatetime()).thenReturn(creationDatetime);
    when(succeededGeoJsonConversionJobMock.getStatus()).thenReturn(jobStatusMock);
    when(succeededGeoJsonConversionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(succeededGeoJsonConversionJobMock.getZoneName()).thenReturn(zoneName);

    assertDoesNotThrow(
        () ->
            subject.accept(
                new GeoJsonConversionAssemblySucceeded(succeededGeoJsonConversionJobMock)));

    verify(detectionFinishedMailerMock, only()).accept(emailReceiver, emailSubject);
  }
}
