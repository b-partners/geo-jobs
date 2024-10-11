package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static app.bpartners.geojobs.endpoint.rest.readme.webhook.ReadmeWebhookValidator.calculateHmacSHA256;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.CreateWebhook;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadmeWebhookValidatorTest {
  ReadmeWebhookConf readmeWebhookConf = new ReadmeWebhookConf();
  ObjectMapper objectMapper = new ObjectMapper();
  ReadmeWebhookValidator subject = new ReadmeWebhookValidator(objectMapper, readmeWebhookConf);

  @BeforeEach
  void setUp() {
    readmeWebhookConf.setSecret("secret");
  }

  @Test
  void accept_ok() throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var body = new CreateWebhook("email", "readmeProject");
    var request = mock(HttpServletRequest.class);
    var validTime = Instant.now().toEpochMilli();
    var validV0 = validTime + "." + objectMapper.writeValueAsString(body);
    var validSignature =
        "t=" + validTime + ",v0=" + calculateHmacSHA256(validV0, readmeWebhookConf.getSecret());
    when(request.getHeader(any())).thenReturn(validSignature);

    assertDoesNotThrow(() -> subject.accept(body, request, readmeWebhookConf));
  }

  @Test
  void accept_throws_error_invalid_signature()
      throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeyException {
    var body = new CreateWebhook("email", "readmeProject");
    var request = mock(HttpServletRequest.class);
    var validTime = Instant.now().toEpochMilli();
    var validV0 = validTime + "." + objectMapper.writeValueAsString(body);
    var validSignature = "t=" + validTime + ",v0=" + calculateHmacSHA256(validV0, "invalid_secret");
    when(request.getHeader(any())).thenReturn(validSignature);

    Exception exception =
        assertThrows(
            ForbiddenException.class, () -> subject.accept(body, request, readmeWebhookConf));
    assertEquals("Webhook not valid", exception.getMessage());
  }
}
