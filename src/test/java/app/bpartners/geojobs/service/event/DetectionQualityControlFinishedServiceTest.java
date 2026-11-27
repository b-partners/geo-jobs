package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.zone.DetectionQualityControlFinished;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.EmailRecipientResolver;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class DetectionQualityControlFinishedServiceTest {
  private static final String CLIENT_EMAIL = "client@example.com";
  private static final String HTML_BODY = "<html>quality control</html>";

  Mailer mailerMock = mock();
  EmailRecipientResolver emailRecipientResolverMock = mock();
  BucketComponent bucketComponentMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  DetectionQualityControlFinishedService subject =
      new DetectionQualityControlFinishedService(
          mailerMock, emailRecipientResolverMock, bucketComponentMock, htmlTemplateParserMock);

  private Detection detection() {
    return Detection.builder()
        .id("detectionId")
        .endToEndId("e2eId")
        .zoneName("zoneName")
        .emailReceiver(CLIENT_EMAIL)
        .geojsonS3FileKey("geojson/key.geojson")
        .build();
  }

  @SneakyThrows
  private void givenResolvedRecipient(Detection detection, String to) {
    when(emailRecipientResolverMock.resolve(detection, CLIENT_EMAIL)).thenReturn(to);
    when(emailRecipientResolverMock.adminCopyUnless(to))
        .thenReturn(List.of(new InternetAddress("admin@example.com")));
    when(bucketComponentMock.presign("geojson/key.geojson"))
        .thenReturn("https://bucket.example.com/key.geojson");
    when(htmlTemplateParserMock.apply(eq("detection_cq_finished_template"), any(Context.class)))
        .thenReturn(HTML_BODY);
  }

  @Test
  @SneakyThrows
  void accept_sends_result_email_to_resolved_recipient_with_admin_copy() {
    var detection = detection();
    givenResolvedRecipient(detection, CLIENT_EMAIL);

    subject.accept(new DetectionQualityControlFinished(detection));

    var captor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock).accept(captor.capture());
    var email = captor.getValue();
    assertEquals(CLIENT_EMAIL, email.to().getAddress());
    assertEquals("admin@example.com", email.cc().getFirst().getAddress());
    assertTrue(email.bcc().isEmpty());
    assertTrue(email.attachments().isEmpty());
    assertEquals(HTML_BODY, email.htmlBody());
    assertTrue(
        email
            .subject()
            .startsWith("Résultat final de la détection portant l'ID e2eId disponible le "));
  }

  @Test
  void accept_fills_template_context_with_detection_and_presigned_geojson_url() {
    var detection = detection();
    givenResolvedRecipient(detection, CLIENT_EMAIL);

    subject.accept(new DetectionQualityControlFinished(detection));

    var captor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock).apply(eq("detection_cq_finished_template"), captor.capture());
    var context = captor.getValue();
    assertEquals("zoneName", context.getVariable("zoneName"));
    assertEquals("e2eId", context.getVariable("detectionE2EId"));
    assertEquals("https://bucket.example.com/key.geojson", context.getVariable("geojsonUrl"));
  }

  @Test
  @SneakyThrows
  void accept_with_invalid_recipient_throws_and_sends_nothing() {
    var detection = detection();
    givenResolvedRecipient(detection, "invalid address@@");

    var event = new DetectionQualityControlFinished(detection);

    assertThrows(RuntimeException.class, () -> subject.accept(event));

    verifyNoInteractions(mailerMock);
  }
}
