package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.FullDetectionSaved;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FullDetectionSavedService implements Consumer<FullDetectionSaved> {
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(FullDetectionSaved fullDetectionSaved) {
    var fullDetection = fullDetectionSaved.getDetection();
    List<InternetAddress> cc = List.of(); // TODO: add admin emails here
    List<InternetAddress> bcc = List.of();
    String subject =
        "FullDetection(id="
            + fullDetection.getId()
            + ", communityOwnerId="
            + fullDetection.getCommunityOwnerId()
            + ") crée le "
            + now();
    String htmlBody = "Éléments fournis par la communauté à afficher";
    List<File> attachments = List.of(); // TODO: add attachments, as provided shape or excel file
    mailer.accept(
        new Email(
            new InternetAddress("tech@bpartners.app"), cc, bcc, subject, htmlBody, attachments));
  }
}
