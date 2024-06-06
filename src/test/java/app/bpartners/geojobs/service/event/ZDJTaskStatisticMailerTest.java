package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.ZDJTaskStatisticMailer.DETECTION_JOB_TYPE;
import static app.bpartners.geojobs.service.ZDJTaskStatisticMailer.TASK_STATISTIC_MAILER_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.ZDJTaskStatisticMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

public class ZDJTaskStatisticMailerTest {
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  ZDJTaskStatisticMailer subject = new ZDJTaskStatisticMailer(mailerMock, htmlTemplateParserMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    String emailBody = "emailBody";
    Instant now = Instant.now();
    String emailReceiver = "dummyEmail";
    String jobId = "jobId";
    String zoneName = "dummyZoneName";
    String env = System.getenv("ENV");
    TaskStatistic taskStatistic = TaskStatistic.builder().updatedAt(now).build();
    ZoneDetectionJob detectionJob =
        ZoneDetectionJob.builder()
            .id(jobId)
            .zoneName(zoneName)
            .emailReceiver(emailReceiver)
            .build();
    String emailSubject =
        "[geo-jobs/"
            + env
            + "] Statistiques du job (id="
            + jobId
            + ", zone="
            + zoneName
            + ", type="
            + DETECTION_JOB_TYPE
            + ") du "
            + now;
    when(htmlTemplateParserMock.apply(any(), any())).thenReturn(emailBody);

    subject.accept(taskStatistic, detectionJob);

    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    var emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(htmlTemplateParserMock, times(1))
        .apply(eq(TASK_STATISTIC_MAILER_TEMPLATE), contextCaptor.capture());
    verify(mailerMock, times(1)).accept(emailCaptor.capture());
    var contextCaptured = contextCaptor.getValue();
    var emailCaptured = emailCaptor.getValue();
    Email expectedEmail =
        new Email(
            new InternetAddress(emailReceiver),
            List.of(),
            List.of(),
            emailSubject,
            emailBody,
            List.of());
    assertEquals(expectedEmail, emailCaptured);
    assertEquals(taskStatistic, contextCaptured.getVariable("taskStatistic"));
    assertEquals(detectionJob, contextCaptured.getVariable("job"));
    assertEquals(DETECTION_JOB_TYPE, contextCaptured.getVariable("jobType"));
  }
}
