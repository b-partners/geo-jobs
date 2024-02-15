package app.bpartners.geojobs.service.tiling;

import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
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
public class TilingFinishedMailer implements Consumer<ZoneTilingJob> {
  private final Mailer mailer;
  public static final String TILING_TEMPLATE_NAME = "zone_tiling";
  private final HTMLTemplateParser htmlTemplateParser;

  @SneakyThrows
  @Override
  public void accept(ZoneTilingJob job) {
    Context context = new Context();
    context.setVariable("zone", job.getZoneName());
    String emailBody = htmlTemplateParser.apply(TILING_TEMPLATE_NAME, context);

    mailer.accept(
        new Email(
            new InternetAddress(job.getEmailReceiver()),
            List.of(),
            List.of(),
            "Résultat du pavage, jobId=" + job.getId(),
            emailBody,
            List.of()));
  }
}
