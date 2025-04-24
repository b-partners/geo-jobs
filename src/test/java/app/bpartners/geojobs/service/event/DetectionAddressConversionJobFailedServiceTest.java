package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionAddressConversionJobFailed;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.DetectionAddressConversionTaskRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionJob;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionAddressConversionJobFailedServiceTest {
  private static final String ADMIN_EMAIL = "admin@email.com";
  DetectionAddressConversionTaskRepository taskRepositoryMock = mock();
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
  BucketComponent bucketComponentMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  DetectionAddressConversionJobFailedService subject =
      new DetectionAddressConversionJobFailedService(
          taskRepositoryMock,
          mailerMock,
          htmlTemplateParser,
          bucketComponentMock,
          detectionRepositoryMock,
          ADMIN_EMAIL);

  @SneakyThrows
  @Test
  void mail_failed_addresses() {
    var jobId = randomUUID().toString();
    var detectionId = randomUUID().toString();
    var detectionE2Id = randomUUID().toString();
    var emailReceiver = "dummy@email.com";
    var excelFileKey = randomUUID().toString();
    var excelFilePreSignedUrl = "http://dummy";
    var jobMock = mock(DetectionAddressConversionJob.class);
    var taskMock = mock(DetectionAddressConversionTask.class);
    var detectionMock = mock(Detection.class);

    when(jobMock.getId()).thenReturn(jobId);
    when(jobMock.getDetectionId()).thenReturn(detectionId);
    when(detectionMock.getEndToEndId()).thenReturn(detectionE2Id);
    when(jobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(detectionMock.getExcelFileKey()).thenReturn(excelFileKey);
    when(taskMock.getAddress()).thenReturn("Malformed address");
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));
    when(taskRepositoryMock.findAllByJobIdAndProgressionStatusAndHealthStatus(
            jobId, "FINISHED", "FAILED"))
        .thenReturn(List.of(taskMock));
    when(bucketComponentMock.presign(excelFileKey, Duration.ofDays(1L)))
        .thenReturn(new URI(excelFilePreSignedUrl).toURL());

    assertDoesNotThrow(() -> subject.accept(new DetectionAddressConversionJobFailed(jobMock)));

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
    var email = emailCaptor.getValue();
    assertEquals(new InternetAddress(emailReceiver), email.to());
    assertEquals(List.of(new InternetAddress(ADMIN_EMAIL)), email.cc());
    assertEquals(List.of(), email.bcc());
    assertTrue(
        email
            .subject()
            .contains(
                "Erreur lors du traitement de 1 adresses durant le traitement"
                    + " de la détection portant l'ID "
                    + detectionE2Id
                    + " le "));
    assertEquals(expectedHtmlBody(detectionE2Id), email.htmlBody());
    assertEquals(List.of(), email.attachments());
  }

  private static @NotNull String expectedHtmlBody(String detectionE2Id) {
    return String.format(
        """
        <html>
        <head>
            <style>
                body {
                    font-family: Helvetica, serif;
                }
            </style>
        </head>
        <body>
        <section>
            <p>Bonjour,</p>
            <p><span>1</span> adresses issus du fichier Excel
                soumis lors de la création de détection portant l'ID = <span>%s</span>, et
                disponible <a
                        href="http://dummy">ici</a>
                n'ont pas pu être traitées :</p>
            <ol>
                <li>Malformed address</li>
            </ol>
            <p>Cordialement.</p>
            <p>L'équipe BPartners.</p>
        </section>
        </body>
        </html>""",
        detectionE2Id);
  }
}
