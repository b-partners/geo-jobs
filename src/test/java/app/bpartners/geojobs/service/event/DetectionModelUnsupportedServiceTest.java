package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionModelUnsupported;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class DetectionModelUnsupportedServiceTest {

  DetectionRepository detectionRepository = mock();
  CommunityAuthorizationRepository communityAuthorizationRepository = mock();
  HTMLTemplateParser htmlTemplateParser = mock();
  Mailer mailer = mock();
  DetectionModelUnsupportedService subject =
      new DetectionModelUnsupportedService(
          detectionRepository, communityAuthorizationRepository, htmlTemplateParser, mailer);

  @Test
  void apply_ok() throws AddressException {

    var detectionIdentifier = "detectionIdentifier";
    DetectionModelUnsupported modelUnsupported =
        new DetectionModelUnsupported()
            .toBuilder().detectionIdentifier(detectionIdentifier).build();
    when(detectionRepository.findById(detectionIdentifier)).thenReturn(Optional.of(detection()));
    when(communityAuthorizationRepository.findById(detection().getCommunityOwnerId()))
        .thenReturn(Optional.of(communityAuthorization()));
    doNothing().when(mailer).accept(any(Email.class));
    when(htmlTemplateParser.apply(any(), any())).thenReturn("</HTML>");

    subject.accept(modelUnsupported);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
    verify(mailer).accept(emailCaptor.capture());
    verify(htmlTemplateParser).apply(any(), contextCaptor.capture());

    var email = emailCaptor.getValue();
    var htmlTemplateContext = contextCaptor.getValue();

    assertTrue(htmlTemplateContext.containsVariable("detection"));
    assertTrue(htmlTemplateContext.containsVariable("communityOwner"));
    assertTrue(htmlTemplateContext.containsVariable("detectionNeedsImageOutput"));
    assertTrue(htmlTemplateContext.containsVariable("detectionOutputType"));
    assertEquals(
        String.format(
            "Détection (e2Id=%s) lancée par %s sur les modèles %s",
            detection().getEndToEndId(),
            communityAuthorization().getName(),
            detection().getDetectableObjectModelList().stream()
                .map(objectModel -> objectModel.getModelName().toString())
                .toList()),
        email.subject());
    assertEquals(new InternetAddress("bpartners.artisans@gmail.com"), email.to());
  }

  private Detection detection() {
    return new Detection()
        .toBuilder()
            .communityOwnerId("communityOwnerId")
            .endToEndId("endToEndId")
            .providedGeoJsonZone(List.of())
            .detectableObjectModel(detectableObjectModelList().getFirst())
            .detectableObjectModelList(detectableObjectModelList())
            .build();
  }

  private CommunityAuthorization communityAuthorization() {
    return CommunityAuthorization.builder().name("CommunityOwner").build();
  }

  private List<DetectableObjectModel> detectableObjectModelList() {
    return List.of(
        new DetectableObjectModel().modelName(ModelName.TOITURE),
        new DetectableObjectModel().modelName(ModelName.VEGETATION));
  }
}
