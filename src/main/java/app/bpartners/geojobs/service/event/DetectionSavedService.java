package app.bpartners.geojobs.service.event;

import static java.time.Instant.now;
import static java.util.Objects.requireNonNullElse;

import app.bpartners.geojobs.endpoint.event.model.DetectionSaved;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.GeoServerParameterStringMapValue;
import app.bpartners.geojobs.service.detection.DetectableObjectModelMapper;
import app.bpartners.geojobs.service.detection.DetectionGeoServerParameterModelMapper;
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
  public static final String DETECTION_SAVED_TEMPLATE = "detection_saved";
  private static final String DEFAULT_PLACEHOLDER = "not provided";
  private final Mailer mailer;
  private final BucketComponent bucketComponent;
  private final DetectableObjectModelMapper detectableObjectModelMapper;
  private final DetectionGeoServerParameterModelMapper detectionGeoServerParameterModelMapper;

  @SneakyThrows
  @Override
  public void accept(DetectionSaved detectionSaved) {
    var detection = detectionSaved.getDetection();
    List<InternetAddress> cc = List.of();
    List<InternetAddress> bcc = List.of();
    var env = System.getenv("ENV");
    String subject =
        String.format(
            "[%s]Detection(id=%s, communityOwnerId=%s) modifiée le %s",
            env == null ? "" : env.toUpperCase(),
            detection.getId(),
            detection.getCommunityOwnerId(),
            now());
    String htmlBody =
        computeStaticEmailBody(
            detection,
            bucketComponent,
            detectableObjectModelMapper,
            detectionGeoServerParameterModelMapper);
    List<File> attachments = List.of();
    mailer.accept(
        new Email(
            new InternetAddress("tech@bpartners.app"), cc, bcc, subject, htmlBody, attachments));
  }

  @NonNull
  public static String computeStaticEmailBody(
      Detection detection,
      BucketComponent bucketComponent,
      DetectableObjectModelMapper detectableObjectModelMapper,
      DetectionGeoServerParameterModelMapper detectionGeoServerParameterModelMapper) {
    var shapeFilePresignURL =
        detection.getShapeFileKey() == null
            ? null
            : bucketComponent
                .presign(detection.getShapeFileKey(), Duration.ofHours(24L))
                .toString();
    var excelFilePresignURL =
        detection.getExcelFileKey() == null
            ? null
            : bucketComponent
                .presign(detection.getExcelFileKey(), Duration.ofHours(24L))
                .toString();
    var modelActualInstance = detection.getDetectableObjectModel().getActualInstance();
    var geoServerProperties = detection.getGeoServerProperties();
    var geoServerUrl = geoServerProperties == null ? null : geoServerProperties.getGeoServerUrl();
    List<GeoServerParameterStringMapValue> geoServerParameter =
        geoServerProperties == null
            ? null
            : detectionGeoServerParameterModelMapper.apply(
                geoServerProperties.getGeoServerParameter());
    HTMLTemplateParser htmlTemplateParser = new HTMLTemplateParser();
    Context context = new Context();
    context.setVariable(
        "detectableObjectModelStringMapValues",
        detectableObjectModelMapper.apply(modelActualInstance));
    context.setVariable(
        "geoServerParameterStringMapValues", requireNonNullElse(geoServerParameter, List.of()));
    context.setVariable("geoServerUrl", requireNonNullElse(geoServerUrl, DEFAULT_PLACEHOLDER));
    context.setVariable("detection", detection);
    context.setVariable("shapeFileUrl", shapeFilePresignURL);
    context.setVariable("excelFileUrl", excelFilePresignURL);

    return htmlTemplateParser.apply(DETECTION_SAVED_TEMPLATE, context);
  }
}
