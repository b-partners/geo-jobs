package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.model.DetectionAreaUnsupported;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class DetectionAreaUnsupportedServiceTest {
  private static final String DETECTION_ID = "detectionId";
  private static final String COMMUNITY_ID = "communityId";
  private static final String E2E_ID = "e2eId";
  private static final String HTML_BODY = "<html>body</html>";

  DetectionRepository detectionRepositoryMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  Mailer mailerMock = mock();
  DetectionAreaUnsupportedService subject =
      new DetectionAreaUnsupportedService(
          detectionRepositoryMock,
          communityAuthorizationRepositoryMock,
          mailerMock,
          htmlTemplateParserMock);

  private static Feature providedFeature() {
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.85")),
                    List.of(new BigDecimal("2.36"), new BigDecimal("48.86")),
                    List.of(new BigDecimal("2.35"), new BigDecimal("48.85")))));
    return Feature.builder()
        .geometry(
            Feature.FeatureGeometry.builder()
                .geometryType(MULTI_POLYGON)
                .actualInstanceStringValue(serialize(new MultiPolygon().coordinates(coordinates)))
                .build())
        .build();
  }

  @SneakyThrows
  private static String serialize(MultiPolygon multiPolygon) {
    return objectMapper().writeValueAsString(multiPolygon);
  }

  private Detection.DetectionBuilder detectionBuilder() {
    return Detection.builder()
        .id(DETECTION_ID)
        .endToEndId(E2E_ID)
        .communityOwnerId(COMMUNITY_ID)
        .providedGeoJsonZone(List.of(providedFeature()));
  }

  private void givenDetection(Detection detection) {
    when(detectionRepositoryMock.findById(DETECTION_ID)).thenReturn(Optional.of(detection));
    when(communityAuthorizationRepositoryMock.findById(COMMUNITY_ID))
        .thenReturn(Optional.of(CommunityAuthorization.builder().name("communityName").build()));
    when(htmlTemplateParserMock.apply(eq("detection_area_unsupported"), any(Context.class)))
        .thenReturn(HTML_BODY);
  }

  private Email sentEmail() {
    var captor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock).accept(captor.capture());
    return captor.getValue();
  }

  @Test
  void accept_builds_subject_and_sends_email_to_tech_team() {
    givenDetection(detectionBuilder().build());

    subject.accept(new DetectionAreaUnsupported(DETECTION_ID, 1_234_567.2));

    var email = sentEmail();
    assertEquals(
        "Surface de la détection (id=e2eId) lancée par communityName excédant 1 km^2",
        email.subject());
    assertEquals(HTML_BODY, email.htmlBody());
    assertEquals("bpartners.artisans@gmail.com", email.to().getAddress());
    assertEquals("tech@birdia.fr", email.cc().getFirst().getAddress());
    assertTrue(email.bcc().isEmpty());
  }

  @Test
  void accept_attaches_provided_zone_as_json_file() {
    givenDetection(detectionBuilder().build());

    subject.accept(new DetectionAreaUnsupported(DETECTION_ID, 1_234_567.2));

    var attachments = sentEmail().attachments();
    assertEquals(1, attachments.size());
    assertTrue(attachments.getFirst().getName().startsWith("detection_e2eId"));
    assertTrue(attachments.getFirst().getName().endsWith(".json"));
  }

  @Test
  void accept_formats_area_in_square_meters_and_rounded_up_square_kilometers() {
    givenDetection(detectionBuilder().build());

    subject.accept(new DetectionAreaUnsupported(DETECTION_ID, 1_234_567.2));

    var context = capturedContext();
    assertEquals("1\u00A0234\u00A0568", context.getVariable("computedAreaInM2"));
    assertEquals(1.24, context.getVariable("computedAreaInKm2"));
  }

  @Test
  void accept_fills_output_labels_from_detection_options() {
    givenDetection(detectionBuilder().isOutputZipped(true).needsImageOutput(true).build());

    subject.accept(new DetectionAreaUnsupported(DETECTION_ID, 2_000_000.0));

    var context = capturedContext();
    assertEquals("Oui", context.getVariable("detectionNeedsImageOutput"));
    assertEquals("ZIP", context.getVariable("detectionOutputType"));
  }

  @Test
  void accept_fills_default_output_labels() {
    givenDetection(detectionBuilder().build());

    subject.accept(new DetectionAreaUnsupported(DETECTION_ID, 2_000_000.0));

    var context = capturedContext();
    assertEquals("Non", context.getVariable("detectionNeedsImageOutput"));
    assertEquals("GeoJson", context.getVariable("detectionOutputType"));
  }

  @Test
  void accept_with_unknown_detection_throws_and_sends_nothing() {
    when(detectionRepositoryMock.findById(DETECTION_ID)).thenReturn(Optional.empty());

    var event = new DetectionAreaUnsupported(DETECTION_ID, 1.0);

    assertThrows(NoSuchElementException.class, () -> subject.accept(event));

    verifyNoInteractions(mailerMock);
  }

  @Test
  void accept_with_unknown_community_throws_and_sends_nothing() {
    when(detectionRepositoryMock.findById(DETECTION_ID))
        .thenReturn(Optional.of(detectionBuilder().build()));
    when(communityAuthorizationRepositoryMock.findById(COMMUNITY_ID)).thenReturn(Optional.empty());

    var event = new DetectionAreaUnsupported(DETECTION_ID, 1.0);

    assertThrows(NoSuchElementException.class, () -> subject.accept(event));

    verifyNoInteractions(mailerMock);
  }

  private Context capturedContext() {
    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock).apply(eq("detection_area_unsupported"), contextCaptor.capture());
    return contextCaptor.getValue();
  }
}
