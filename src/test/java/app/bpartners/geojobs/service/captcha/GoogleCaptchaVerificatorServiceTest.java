package app.bpartners.geojobs.service.captcha;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.google.GoogleCaptchaResponse;
import app.bpartners.geojobs.service.google.captcha.GoogleCaptchaVerificatorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class GoogleCaptchaVerificatorServiceTest {
  RestTemplate restTemplate = mock(RestTemplate.class);
  String secretToken = "dummy";
  String baseUrlMock = "https://www.google.com/";
  GoogleCaptchaVerificatorService subject =
      new GoogleCaptchaVerificatorService(restTemplate, secretToken, baseUrlMock);

  @Test
  void verifyCaptcha() {
    GoogleCaptchaResponse mockResponse =
        GoogleCaptchaResponse.builder().success(true).score(0.6).build();
    ResponseEntity<GoogleCaptchaResponse> responseEntity =
        new ResponseEntity<>(mockResponse, HttpStatus.OK);
    when(restTemplate.postForEntity(eq(baseUrlMock), any(), eq(GoogleCaptchaResponse.class)))
        .thenReturn(responseEntity);

    boolean isVerified = subject.verifyToken("dummy");

    assertEquals(true, isVerified);
  }
}
