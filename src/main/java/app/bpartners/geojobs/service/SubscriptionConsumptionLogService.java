package app.bpartners.geojobs.service;

import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.model.SubscriptionConsumptionLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SubscriptionConsumptionLogService {
  private static final String API_KEY_HEADER = "x-api-key";
  private final RestTemplate restTemplate;
  private final AuthProvider authProvider;

  private final String bpartnersApiUrl;

  public SubscriptionConsumptionLogService(
      AuthProvider authProvider, @Value("${bpartners.api.url}") String bpartnersApiUrl) {
    this.restTemplate = new RestTemplate();
    this.authProvider = authProvider;
    this.bpartnersApiUrl = bpartnersApiUrl;
  }

  public SubscriptionConsumptionLog sendSubscriptionConsumptionLog(
      String userId, SubscriptionConsumptionLog subscriptionConsumptionLog) {
    var apiKey = authProvider.getPrincipal().getApiKey();
    String apiUrl =
        String.format("%s/users/%s/subscriptionConsumptionLogs", bpartnersApiUrl, userId);
    HttpHeaders headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    HttpEntity<SubscriptionConsumptionLog> payload =
        new HttpEntity<>(subscriptionConsumptionLog, headers);
    return restTemplate.postForObject(apiUrl, payload, SubscriptionConsumptionLog.class);
  }
}
