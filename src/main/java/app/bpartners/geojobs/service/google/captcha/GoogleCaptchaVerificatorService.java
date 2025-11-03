package app.bpartners.geojobs.service.google.captcha;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import app.bpartners.geojobs.model.google.GoogleCaptchaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleCaptchaVerificatorService {
  private final String baseUrl;
  private RestTemplate restTemplate;
  private String recaptchaSecret;

  public GoogleCaptchaVerificatorService(
      RestTemplate restTemplate,
      @Value("${google.captcha.secret}") String recaptchaSecret,
      @Value("${google.captcha.url}") String recaptchaBaseUrl) {
    this.restTemplate = restTemplate;
    this.recaptchaSecret = recaptchaSecret;
    this.baseUrl = recaptchaBaseUrl;
  }

  public boolean verifyToken(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(APPLICATION_FORM_URLENCODED);

    String body = "secret=" + recaptchaSecret + "&response=" + token;
    HttpEntity<String> request = new HttpEntity<>(body, headers);

    ResponseEntity<GoogleCaptchaResponse> response =
        restTemplate.postForEntity(baseUrl, request, GoogleCaptchaResponse.class);

    GoogleCaptchaResponse data = response.getBody();

    return data != null && data.isSuccess() && data.getScore() != null && data.getScore() > 0.5;
  }
}
