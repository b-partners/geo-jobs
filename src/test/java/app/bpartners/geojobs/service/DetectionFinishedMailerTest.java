package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionFinishedMailerTest {
  Mailer mailerMock = mock(Mailer.class);
  DetectionFinishedMailer subject = new DetectionFinishedMailer(mailerMock);

  @SneakyThrows
  @Test
  void send_email() {
    Instant now = Instant.parse("2025-01-01T07:00:00Z");
    String emailReceiver = "emailReceiver";
    String zoneName = "zoneName";

    assertDoesNotThrow(() -> subject.accept(emailReceiver, zoneName, now));

    // TODO : Paris is GMT+1 now but must be set to +2 from 31st march 2025
    var expectedSubject = "Analyse sur la zone zoneName terminée le 01/01/2025 08:00:00";
    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock, only()).accept(emailCaptor.capture());
    var actualEmail = emailCaptor.getValue();
    assertEquals(
        new Email(
            new InternetAddress(emailReceiver),
            List.of(new InternetAddress("tech@bpartners.app")),
            List.of(),
            expectedSubject,
            null,
            List.of()),
        actualEmail);
  }
}
