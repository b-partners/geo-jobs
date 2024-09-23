package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionSavedService implements Consumer<DetectionSaved> {
  private final Mailer mailer;
  private final BucketComponent bucketComponent;

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
            + ") modifiée le "
            + now();
    // TODO: set as html parsed body
    String htmlBody = computeStaticEmailBody(detection, bucketComponent);
    List<File> attachments = List.of(); // TODO: add attachments, as provided shape or excel file
    mailer.accept(
        new Email(
            new InternetAddress("tech@bpartners.app"), cc, bcc, subject, htmlBody, attachments));
  }

  @NonNull
  public static String computeStaticEmailBody(
      Detection detection, BucketComponent bucketComponent) {
    String htmlBody =
        "Éléments fournis par le consommateur d'API : \n"
            + "Configurations sur la détection : "
            + detection.getDetectableObjectConfigurations()
            + "\n"
            + "Configuration globale : "
            + detection.getDetectableObjectConfigurations()
            + "\n";
    if (detection.getGeoJsonZone() != null && !detection.getGeoJsonZone().isEmpty()) {
      htmlBody += "Zone en GeoJson fournis : " + detection.getGeoJsonZone();
    }
    if (detection.getShapeFileKey() != null) {
      var shapeFilePresignURL =
          bucketComponent.presign(detection.getShapeFileKey(), Duration.ofHours(2L)).toString();
      htmlBody += "Fichier shape à traiter : " + shapeFilePresignURL;
    }
    return htmlBody;
  }
}
