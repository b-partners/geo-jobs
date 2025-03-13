package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.SubscriptionConsumptionLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SubscriptionConsumptionLogService {
  private final RestTemplate restTemplate;
  private final AuthProvider authProvider;

  private final String bpartnersApiUrl;

  public SubscriptionConsumptionLogService(
      RestTemplate restTemplate,
      AuthProvider authProvider,
      @Value("${bpartnersApi.url}") String bpartnersApiUrl) {
    this.restTemplate = restTemplate;
    this.authProvider = authProvider;
    this.bpartnersApiUrl = bpartnersApiUrl;
  }

  public SubscriptionConsumptionLog addSubscriptionConsumptionLog(
      String userId, SubscriptionConsumptionLog subscriptionConsumptionLog) {
    var apiKey = authProvider.getPrincipal().getApiKey();
    String url = String.format("%s/users/%s/subscriptionConsumptionLogs", bpartnersApiUrl, userId);
    HttpHeaders headers = new HttpHeaders();
    headers.set("x-api-key", apiKey);
    HttpEntity<SubscriptionConsumptionLog> entity =
        new HttpEntity<>(subscriptionConsumptionLog, headers);
    ResponseEntity<SubscriptionConsumptionLog> response =
        restTemplate.exchange(url, HttpMethod.POST, entity, SubscriptionConsumptionLog.class);
    return response.getBody();
  }
}
