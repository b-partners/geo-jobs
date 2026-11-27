package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.STATIONNEMENT;
import static app.bpartners.geojobs.endpoint.rest.model.ModelName.TOITURE;
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

import app.bpartners.geojobs.endpoint.event.model.DetectionModelUnsupported;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
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

class DetectionModelUnsupportedServiceTest {
  private static final String DETECTION_ID = "detectionId";
  private static final String COMMUNITY_ID = "communityId";
  private static final String E2E_ID = "e2eId";
  private static final String HTML_BODY = "<html>body</html>";

  DetectionRepository detectionRepositoryMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  HTMLTemplateParser htmlTemplateParserMock = mock();
  Mailer mailerMock = mock();
  DetectionModelUnsupportedService subject =
      new DetectionModelUnsupportedService(
          detectionRepositoryMock,
          communityAuthorizationRepositoryMock,
          htmlTemplateParserMock,
          mailerMock);

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
    when(htmlTemplateParserMock.apply(eq("detection_model_unsupported"), any(Context.class)))
        .thenReturn(HTML_BODY);
  }

  private Email sentEmail() {
    var captor = ArgumentCaptor.forClass(Email.class);
    verify(mailerMock).accept(captor.capture());
    return captor.getValue();
  }

  @Test
  void accept_with_several_models_lists_all_model_names_in_subject() {
    givenDetection(
        detectionBuilder()
            .detectableObjectModelList(
                List.of(
                    new DetectableObjectModel().modelName(TOITURE),
                    new DetectableObjectModel().modelName(STATIONNEMENT)))
            .build());

    subject.accept(new DetectionModelUnsupported(DETECTION_ID));

    var email = sentEmail();
    assertEquals(
        "Détection (e2Id=e2eId) lancée par communityName sur les modèles [BP_TOITURE,"
            + " BP_STATIONNEMENT]",
        email.subject());
    assertEquals(HTML_BODY, email.htmlBody());
  }

  @Test
  void accept_with_single_model_uses_singular_subject() {
    givenDetection(
        detectionBuilder()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .build());

    subject.accept(new DetectionModelUnsupported(DETECTION_ID));

    assertEquals(
        "Détection (e2Id=e2eId) lancée par communityName sur le modèle BP_TOITURE",
        sentEmail().subject());
  }

  @Test
  void accept_sends_to_tech_team_with_provided_zone_attachment() {
    givenDetection(
        detectionBuilder()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .build());

    subject.accept(new DetectionModelUnsupported(DETECTION_ID));

    var email = sentEmail();
    assertEquals("bpartners.artisans@gmail.com", email.to().getAddress());
    assertEquals("tech@birdia.fr", email.cc().getFirst().getAddress());
    assertTrue(email.bcc().isEmpty());
    assertEquals(1, email.attachments().size());
    assertTrue(email.attachments().getFirst().getName().startsWith("detection_e2eId"));
    assertTrue(email.attachments().getFirst().getName().endsWith(".geojson"));
  }

  @Test
  void accept_fills_template_context_with_output_labels() {
    givenDetection(
        detectionBuilder()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .isOutputZipped(true)
            .needsImageOutput(true)
            .build());

    subject.accept(new DetectionModelUnsupported(DETECTION_ID));

    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock)
        .apply(eq("detection_model_unsupported"), contextCaptor.capture());
    assertEquals("Oui", contextCaptor.getValue().getVariable("detectionNeedsImageOutput"));
    assertEquals("ZIP", contextCaptor.getValue().getVariable("detectionOutputType"));
  }

  @Test
  void accept_fills_template_context_with_default_output_labels() {
    givenDetection(
        detectionBuilder()
            .detectableObjectModel(new DetectableObjectModel().modelName(TOITURE))
            .build());

    subject.accept(new DetectionModelUnsupported(DETECTION_ID));

    var contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(htmlTemplateParserMock)
        .apply(eq("detection_model_unsupported"), contextCaptor.capture());
    assertEquals("Non", contextCaptor.getValue().getVariable("detectionNeedsImageOutput"));
    assertEquals("GeoJson", contextCaptor.getValue().getVariable("detectionOutputType"));
  }

  @Test
  void accept_with_unknown_detection_throws_and_sends_nothing() {
    when(detectionRepositoryMock.findById(DETECTION_ID)).thenReturn(Optional.empty());

    var event = new DetectionModelUnsupported(DETECTION_ID);

    assertThrows(NoSuchElementException.class, () -> subject.accept(event));

    verifyNoInteractions(mailerMock);
  }

  @Test
  void accept_with_unknown_community_throws_and_sends_nothing() {
    when(detectionRepositoryMock.findById(DETECTION_ID))
        .thenReturn(Optional.of(detectionBuilder().build()));
    when(communityAuthorizationRepositoryMock.findById(COMMUNITY_ID)).thenReturn(Optional.empty());

    var event = new DetectionModelUnsupported(DETECTION_ID);

    assertThrows(NoSuchElementException.class, () -> subject.accept(event));

    verifyNoInteractions(mailerMock);
  }
}
