package app.bpartners.geojobs.service.geo.detection;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.Status;
import app.bpartners.geojobs.repository.model.geo.detection.ZoneDetectionJob;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class DetectionFinishedMailer implements Consumer<ZoneDetectionJob> {
  private final Mailer mailer;
  public static final String ZDJ_FINISHED_TEMPLATE = "machine_zdj_finished";
  private final HTMLTemplateParser htmlTemplateParser;

  @SneakyThrows
  @Override
  public void accept(ZoneDetectionJob job) {
    Context context = new Context();
    context.setVariable("job", job);
    context.setVariable("jobHealthStatus", healthStatusFr(job.getStatus().getHealth()));
    String emailBody = htmlTemplateParser.apply(ZDJ_FINISHED_TEMPLATE, context);

    mailer.accept(
        new Email(
            new InternetAddress(job.getEmailReceiver()),
            List.of(),
            List.of(),
            "Tâche de détection-machine [ID=" + job.getId() + "] terminée",
            emailBody,
            List.of()));
  }

  private String healthStatusFr(Status.HealthStatus status) {
    switch (status) {
      case SUCCEEDED:
        return "succès";
      case FAILED:
        return "échec";
      case UNKNOWN:
        return "inconnu";
      default:
        throw new ApiException(
            ApiException.ExceptionType.SERVER_EXCEPTION, "Unknown job health status " + status);
    }
  }
}
