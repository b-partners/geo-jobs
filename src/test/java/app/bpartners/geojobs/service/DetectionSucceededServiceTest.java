package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.POST_PROCESSING;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.DetectionSucceeded;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.DetectionStep;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.event.DetectionSucceededService;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import app.bpartners.geojobs.utils.PassThroughEmailRecipientResolver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class DetectionSucceededServiceTest {
  DetectionFinishedMailer detectionFinishedMailerMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  ZoneDetectionJobRepository zoneDetectionJobRepositoryMock = mock();
  DetectionSucceededService subject =
      new DetectionSucceededService(
          detectionFinishedMailerMock,
          htmlTemplateParserMock,
          detectionRepositoryMock,
          bucketComponentMock,
          zoneDetectionJobRepositoryMock,
          new PassThroughEmailRecipientResolver());

  @Test
  void throw_not_implemented_when_detection_step_not_persisted() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getStep()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));

    var actualException =
        assertThrows(
            NotImplementedException.class,
            () -> subject.accept(new DetectionSucceeded(detectionIdentifier)));

    assertEquals(
        "Only detection with persisted step can be handled by this service for now."
            + " Detection.id="
            + detectionIdentifier
            + " does not have persisted step",
        actualException.getMessage());
  }

  @Test
  void throw_illegal_state_exception_when_detection_step_not_post_processing_finished_succeeded() {
    var detectionIdentifier = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    var actualDetectionStep =
        DetectionStep.builder()
            .detectionId(detectionIdentifier)
            .name(MACHINE_DETECTION)
            .progression(FINISHED)
            .health(SUCCEEDED)
            .build();
    when(detectionMock.getStep()).thenReturn(actualDetectionStep);
    when(detectionMock.isOnStepPostProcessingSucceeded()).thenReturn(false);
    when(detectionRepositoryMock.findById(detectionIdentifier))
        .thenReturn(Optional.of(detectionMock));

    var actualException =
        assertThrows(
            IllegalStateException.class,
            () -> subject.accept(new DetectionSucceeded(detectionIdentifier)));

    assertEquals(
        "Only detection on step POST_PROCESSING (FINISHED - SUCCEEDED) can be processed. Otherwise,"
            + " actual Detection.id="
            + detectionIdentifier
            + " has step "
            + actualDetectionStep,
        actualException.getMessage());
  }

  private static final Instant SUCCEEDED_AT = Instant.parse("2025-01-15T10:30:00Z");
  private static final String SUCCEEDED_AT_PARIS = "15/01/2025 11:30";

  private Detection succeededDetection(boolean integrationTest) {
    var detection =
        Detection.builder()
            .id("detectionId")
            .endToEndId("e2eId")
            .zdjId("zdjId")
            .zoneName("zoneName")
            .emailReceiver("client@example.com")
            .geojsonS3FileKey("geojson/key.geojson")
            .integrationTest(integrationTest)
            .build();
    detection.addStep(
        DetectionStep.builder()
            .detectionId("detectionId")
            .name(POST_PROCESSING)
            .progression(FINISHED)
            .health(SUCCEEDED)
            .creationDatetime(SUCCEEDED_AT)
            .build());
    return detection;
  }

  private Context renderedContext() {
    var captor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock).apply(eq("detection_succeeded_template"), captor.capture());
    return captor.getValue();
  }

  @Test
  void accept_sends_succeeded_email_with_job_and_detection_context() {
    var detection = succeededDetection(false);
    var machineZdj =
        ZoneDetectionJob.builder()
            .id("zdjId")
            .statusHistory(
                List.of(
                    JobStatus.builder()
                        .progression(FINISHED)
                        .health(SUCCEEDED)
                        .creationDatetime(Instant.parse("2025-01-15T09:00:00Z"))
                        .build()))
            .build();
    when(detectionRepositoryMock.findById("detectionId")).thenReturn(Optional.of(detection));
    when(detectionRepositoryMock.findByZdjId("zdjId")).thenReturn(Optional.of(detection));
    when(zoneDetectionJobRepositoryMock.findById("zdjId")).thenReturn(Optional.of(machineZdj));
    when(bucketComponentMock.presign("geojson/key.geojson")).thenReturn("https://geojson.url");
    when(htmlTemplateParserMock.apply(eq("detection_succeeded_template"), any(Context.class)))
        .thenReturn("html body");

    subject.accept(new DetectionSucceeded("detectionId"));

    verify(detectionFinishedMailerMock)
        .accept(
            "client@example.com",
            "Analyse sur la zone zoneName terminée le " + SUCCEEDED_AT_PARIS,
            "html body");
    var context = renderedContext();
    assertEquals("zdjId", context.getVariable("zoneDetectionJobId"));
    assertEquals(detection, context.getVariable("detection"));
    assertEquals(machineZdj, context.getVariable("zoneDetectionJob"));
    assertEquals("https://geojson.url", context.getVariable("geojsonUrl"));
    assertEquals(SUCCEEDED_AT_PARIS, context.getVariable("detectionSucceededDatetime"));
    assertEquals("15/01/2025 10:00", context.getVariable("zoneDetectionJobSucceededDatetime"));
  }

  @Test
  void accept_without_zone_detection_job_nor_detection_renders_minimal_context() {
    subject.accept(
        null,
        JobStatus.builder().creationDatetime(SUCCEEDED_AT).build(),
        "zoneName",
        SUCCEEDED_AT_PARIS,
        "client@example.com");

    var context = renderedContext();
    assertNull(context.getVariable("zoneDetectionJob"));
    assertNull(context.getVariable("detection"));
    assertNull(context.getVariable("geojsonUrl"));
    assertNull(context.getVariable("zoneDetectionJobSucceededDatetime"));
    verify(detectionFinishedMailerMock)
        .accept(
            eq("client@example.com"),
            eq("Analyse sur la zone zoneName terminée le " + SUCCEEDED_AT_PARIS),
            any());
  }

  @Test
  void accept_does_not_send_email_for_integration_test_detection() {
    var detection = succeededDetection(true);
    when(detectionRepositoryMock.findById("detectionId")).thenReturn(Optional.of(detection));

    subject.accept(new DetectionSucceeded("detectionId"));

    verifyNoInteractions(detectionFinishedMailerMock);
  }

  @Test
  void accept_reused_sends_email_with_detection_only_context() {
    var detection = succeededDetection(false);
    when(bucketComponentMock.presign("geojson/key.geojson")).thenReturn("https://geojson.url");
    when(htmlTemplateParserMock.apply(eq("detection_succeeded_template"), any(Context.class)))
        .thenReturn("reused body");

    subject.acceptReused(detection);

    verify(detectionFinishedMailerMock)
        .accept(
            eq("client@example.com"),
            startsWith("Analyse sur la zone zoneName terminée le "),
            eq("reused body"));
    var context = renderedContext();
    assertEquals(detection, context.getVariable("detection"));
    assertNull(context.getVariable("zoneDetectionJob"));
    assertNull(context.getVariable("zoneDetectionJobId"));
    assertEquals("https://geojson.url", context.getVariable("geojsonUrl"));
  }

  @Test
  void accept_reused_does_not_send_email_for_integration_test_detection() {
    subject.acceptReused(succeededDetection(true));

    verifyNoInteractions(detectionFinishedMailerMock);
    verifyNoInteractions(htmlTemplateParserMock);
  }
}
