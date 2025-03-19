package app.bpartners.geojobs.service;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionFinishedMailer implements TriConsumer<String, String, Instant> {
  private final Mailer mailer;

  @Override
  @SneakyThrows
  public void accept(String emailReceiver, String zoneName, Instant datetime) {
    var formattedCreationDatetime =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .format(datetime.atZone(ZoneId.of("Europe/Paris")));

    mailer.accept(
        new Email(
            new InternetAddress(emailReceiver),
            List.of(new InternetAddress("tech@bpartners.app")),
            List.of(),
            String.format(
                "Analyse sur la zone %s terminée le %s", zoneName, formattedCreationDatetime),
            null,
            List.of()));
  }
}
