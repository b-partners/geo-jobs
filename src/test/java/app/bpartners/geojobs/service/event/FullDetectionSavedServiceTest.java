package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.FullDetectionSaved;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FullDetectionSavedServiceTest {
  Mailer mailerMock = mock();
  FullDetectionSavedService subject = new FullDetectionSavedService(mailerMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    var fullDetection = new FullDetection();
    List<InternetAddress> cc = List.of(); // TODO: add admin emails here
    List<InternetAddress> bcc = List.of();
    String htmlBody = "Éléments fournis par la communauté à afficher";
    List<File> attachments = List.of(); // TODO: add attachments, as provided shape or excel file

    subject.accept(FullDetectionSaved.builder().fullDetection(fullDetection).build());

    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
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
  }
}