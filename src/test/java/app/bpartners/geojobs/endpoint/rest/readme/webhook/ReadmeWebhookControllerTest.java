package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookValidator.calculateHmacSHA256;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.ReadmeWebhookController;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.CreateWebhook;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SingleUserInfo;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadmeWebhookControllerTest {
  ObjectMapper objectMapper = new ObjectMapper();
  ReadmeWebhookConf conf = new ReadmeWebhookConf();
  ReadmeWebhookValidator validator = new ReadmeWebhookValidator(new ObjectMapper(), conf);
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock =
      mock(CommunityAuthorizationRepository.class);
  AuthProvider authProvider = mock(AuthProvider.class);
  ReadmeWebhookService service =
      new ReadmeWebhookService("admin-email", communityAuthorizationRepositoryMock, authProvider);
  ReadmeWebhookController subject = new ReadmeWebhookController(validator, conf, service);

  @Test
  void readme_webhook_ok()
      throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
    conf.setSecret("secret");
    var body = new CreateWebhook("email", "readmeProject");
    var request = mock(HttpServletRequest.class);
    var validTime = Instant.now().toEpochMilli();
    var validV0 = validTime + "." + objectMapper.writeValueAsString(body);
    var validSignature = "t=" + validTime + ",v0=" + calculateHmacSHA256(validV0, "secret");
    var communityAuthorization =
        CommunityAuthorization.builder().email(body.email()).name("").apiKey("").build();
    when(communityAuthorizationRepositoryMock.findByEmail(any()))
        .thenReturn(Optional.of(communityAuthorization));
    when(request.getHeader(any())).thenReturn(validSignature);

    var actual = subject.readmeWebhook(body, request);

    var expected =
        SingleUserInfo.builder().email(body.email()).name("").apiKey("").isAdmin(false).build();
    assertEquals(expected, actual);
  }
}
