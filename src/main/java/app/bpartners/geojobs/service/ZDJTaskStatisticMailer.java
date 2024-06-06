package app.bpartners.geojobs.service;

import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class ZDJTaskStatisticMailer implements BiConsumer<TaskStatistic, ZoneDetectionJob> {
  public static final String DETECTION_JOB_TYPE = "DETECTION";
  public static final String TASK_STATISTIC_MAILER_TEMPLATE = "zdj_task_statistic";
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final String env = System.getenv("ENV");

  @SneakyThrows
  @Override
  public void accept(TaskStatistic taskStatistic, ZoneDetectionJob detectionJob) {
    Context context = new Context();
    context.setVariable("taskStatistic", taskStatistic);
    context.setVariable("job", detectionJob);
    context.setVariable("jobType", DETECTION_JOB_TYPE);
    String emailBody = htmlTemplateParser.apply(TASK_STATISTIC_MAILER_TEMPLATE, context);
    String subject =
        "[geo-jobs/"
            + env
            + "] Statistiques du job (id="
            + detectionJob.getId()
            + ", zone="
            + detectionJob.getZoneName()
            + ", type="
            + DETECTION_JOB_TYPE
            + ") du "
            + taskStatistic.getUpdatedAt();
    mailer.accept(
        new Email(
            new InternetAddress(detectionJob.getEmailReceiver()),
            List.of(),
            List.of(),
            subject,
            emailBody,
            List.of()));
  }
}
