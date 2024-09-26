package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.BPToitureModel.ModelNameEnum.BP_TOITURE;
import static app.bpartners.geojobs.service.event.DetectionSavedService.computeStaticEmailBody;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionSavedServiceTest {
  BucketComponent bucketComponentMock = mock();
  Mailer mailerMock = mock();
  DetectionSavedService subject = new DetectionSavedService(mailerMock, bucketComponentMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    when(bucketComponentMock.presign(any(), any())).thenReturn(new URI("http://localhost").toURL());
    var shapeFileKey = "dummy";
    var detection =
        Detection.builder()
            .shapeFileKey(shapeFileKey)
            .bpToitureModel(new BPToitureModel().modelName(BP_TOITURE))
            .build();
    List<InternetAddress> cc = List.of();
    List<InternetAddress> bcc = List.of();
    String htmlBody = computeStaticEmailBody(detection, bucketComponentMock);
    List<File> attachments = List.of();

    subject.accept(DetectionSaved.builder().detection(detection).build());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    var durationCaptor = ArgumentCaptor.forClass(Duration.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
    verify(bucketComponentMock, times(2)).presign(eq(shapeFileKey), durationCaptor.capture());
    var urlDurationValue = durationCaptor.getValue();
    var actualEmail = emailCaptor.getValue();
    var expectedMail =
        new Email(
            new InternetAddress("tech@bpartners.app"),
            cc,
            bcc,
            actualEmail.subject(),
            htmlBody,
            attachments);
    assertEquals(expectedMail, actualEmail);
    assertEquals(Duration.ofHours(24L), urlDurationValue);
  }
}
