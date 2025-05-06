package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.CreateApiKey.ConsumerTypeEnum.INSURANCE;
import static app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.controller.SecurityController;
import app.bpartners.geojobs.endpoint.rest.model.CreateApiKey;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityDetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecurityControllerIT extends FacadeIT {
  @Autowired SecurityController subject;
  @Autowired CommunityAuthorizationRepository authorizationRepository;

  @Test
  void generate_api_keys_for_insurance_ok() {
    var actual = subject.generateApiKeys(List.of(someCreateApiKey("randomEmail" + randomUUID())));

    assertEquals(1, actual.size());
    var actualKey = actual.getFirst().getKey();
    var actualCommunity = authorizationRepository.findByApiKey(actualKey).orElse(null);
    assertNotNull(actualCommunity);
    assertTrue(
        actualCommunity.getDetectableObjectTypes().stream()
            .map(CommunityDetectableObjectType::getType)
            .toList()
            .containsAll(
                List.of(
                    DetectableType.TOITURE_REVETEMENT,
                    DetectableType.HUMIDITE_INTENSE,
                    DetectableType.USURE_IMPORTANTE,
                    DetectableType.MOISISSURE_NOIRCIE)));
    assertTrue(actualCommunity.getAuthorizedZones().isEmpty());
  }

  @Test
  void used_email_throws_ko() {
    var consumerEmail = "randomEmail" + randomUUID();
    assertDoesNotThrow(() -> subject.generateApiKeys(List.of(someCreateApiKey(consumerEmail))));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.generateApiKeys(List.of(someCreateApiKey(consumerEmail))));

    assertEquals("Email=" + consumerEmail + " is already used", actual.getMessage());
  }

  private static CreateApiKey someCreateApiKey(String consumerEmail) {
    return new CreateApiKey()
        .consumerName("dummyConsumerName")
        .consumerEmail(consumerEmail)
        .consumerType(INSURANCE)
        .detectableObjectTypes(
            List.of(TOITURE_REVETEMENT, HUMIDITE_INTENSE, USURE_IMPORTANTE, MOISISSURE_NOIRCIE))
        .maxSurface(null)
        .authorizedZones(List.of());
  }
}
