package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
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
public class DetectionSavedService implements Consumer<DetectionSaved> {
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(DetectionSaved detectionSaved) {
    var detection = detectionSaved.getDetection();
    List<InternetAddress> cc = List.of(); // TODO: add admin emails here
    List<InternetAddress> bcc = List.of();
    String subject =
        "Detection(id="
            + detection.getId()
            + ", communityOwnerId="
            + detection.getCommunityOwnerId()
            + ") crée le "
            + now();
    String htmlBody = "Éléments fournis par la communauté à afficher";
    List<File> attachments = List.of(); // TODO: add attachments, as provided shape or excel file
    mailer.accept(
        new Email(
            new InternetAddress("tech@bpartners.app"), cc, bcc, subject, htmlBody, attachments));
  }
}
