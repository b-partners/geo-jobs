package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.GeoJobType.DETECTION;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

public class JobFinishedMailerTest {
  public static final String JOB_ID = "jobId";
  Mailer mailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  JobFinishedMailer<ZoneDetectionJob> detectionSubject =
      new JobFinishedMailer<>(mailerMock, htmlTemplateParserMock);

  @Test
  void accept_ok() {
    ZoneDetectionJob job =
        ZoneDetectionJob.builder()
            .id(JOB_ID)
            .emailReceiver("dummy@email.com")
            .statusHistory(List.of(JobStatus.builder().jobType(DETECTION).build()))
            .build();

    assertDoesNotThrow(() -> detectionSubject.accept(job));

    var contextArgCaptor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock, times(1)).apply(eq("job_finished"), contextArgCaptor.capture());
    var contextValue = contextArgCaptor.getValue();
    assertEquals(job, contextValue.getVariable("job"));
    assertEquals(job.getStatus().toString(), contextValue.getVariable("status"));
  }
}
