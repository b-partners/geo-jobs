package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class DetectionSavedService implements Consumer<DetectionSaved> {
  private static final String DETECTION_SAVED_TEMPLATE = "detection_saved";
  private final Mailer mailer;
  private final BucketComponent bucketComponent;

  @SneakyThrows
  @Override
  public void accept(DetectionSaved detectionSaved) {
    var detection = detectionSaved.getDetection();
    List<InternetAddress> cc = List.of();
    List<InternetAddress> bcc = List.of();
    String subject =
        "Detection(id="
            + detection.getId()
            + ", communityOwnerId="
            + detection.getCommunityOwnerId()
            + ") modifiée le "
            + now();
    String htmlBody = computeStaticEmailBody(detection, bucketComponent);
    List<File> attachments = List.of();
    mailer.accept(
        new Email(
            new InternetAddress("tech@bpartners.app"), cc, bcc, subject, htmlBody, attachments));
  }

  @NonNull
  public static String computeStaticEmailBody(
      Detection detection, BucketComponent bucketComponent) {
    var shapeFilePresignURL =
        detection.getShapeFileKey() == null
            ? null
            : bucketComponent
                .presign(detection.getShapeFileKey(), Duration.ofHours(24L))
                .toString();
    var excelFilePresignURL =
        detection.getShapeFileKey() == null
            ? null
            : bucketComponent
                .presign(detection.getExcelFileKey(), Duration.ofHours(24L))
                .toString();
    HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
    Context context = new Context();
    context.setVariable("detection", detection);
    context.setVariable("shapeFileUrl", shapeFilePresignURL);
    context.setVariable("excelFileUrl", excelFilePresignURL);
    return htmlTemplateParser.apply(DETECTION_SAVED_TEMPLATE, context);
  }
}
