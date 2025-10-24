package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.DetectionFinishedMailer.ADMIN_EMAIL;

import app.bpartners.geojobs.endpoint.event.model.zone.FileResultCQEmailInitiated;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@Slf4j
@AllArgsConstructor
public class FileResultCQEmailInitiatedService implements Consumer<FileResultCQEmailInitiated> {
  private Mailer mailer;
  private BucketComponent bucketComponent;
  private HTMLTemplateParser htmlTemplateParser;
  private final String FILE_RESULT_SUBMISSION_FINISHED = "detection_cq_finished_template";

  @Override
  public void accept(FileResultCQEmailInitiated fileResultCQEmailInitiated) {
    var detection = fileResultCQEmailInitiated.getDetection();
    String emailSubject =
        String.format(
            "[Contrôle Qualité] Résultat de l'analyse après contrôle qualité sur la zone %s",
            detection.getZoneName());

    try {
      mailer.accept(
          new Email(
              new InternetAddress(detection.getEmailReceiver()),
              List.of(new InternetAddress(ADMIN_EMAIL)),
              List.of(),
              emailSubject,
              apply(detection),
              List.of()));
    } catch (AddressException e) {
      throw new RuntimeException(e);
    }

    log.info("CQ File result email sent");
  }

  private String apply(Detection detection) {
    Context context = new Context();
    context.setVariable("zoneName", detection.getZoneName());
    context.setVariable("detectionE2EId", detection.getEndToEndId());
    context.setVariable("geojsonUrl", bucketComponent.presign(detection.getGeojsonS3FileKey()));

    return htmlTemplateParser.apply(FILE_RESULT_SUBMISSION_FINISHED, context);
  }
}
