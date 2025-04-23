package app.bpartners.geojobs.service.dashboard;

import static app.bpartners.geojobs.service.dashboard.ApiConfiguration.API_KEY_HEADER;
import static org.springframework.http.HttpMethod.GET;

import app.bpartners.geojobs.service.dashboard.component.Account;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserAccountsApi {
  private final RestTemplate restTemplate = new RestTemplate();
  private final ApiConfiguration apiConfiguration;
  private final SecurityApi securityApi;

  public List<Account> getAccountsByUserId(String userId, String apiKey) {
    String endpoint =
        String.format("%s/users/%s/accounts", apiConfiguration.getDashboardApiUrl(), userId);
    var headers = new HttpHeaders();
    headers.add(API_KEY_HEADER, apiKey);
    var requestEntity = new HttpEntity<>(headers);

    return restTemplate
        .exchange(endpoint, GET, requestEntity, new ParameterizedTypeReference<List<Account>>() {})
        .getBody();
  }

  public Account getActiveByUserId(String apiKey) {
    var accounts = getAccountsByUserId(securityApi.retrieveUserId(apiKey), apiKey);
    return accounts.stream()
        .filter(Account::active)
        .findFirst()
        .orElseGet(() -> accounts.size() > 1 ? accounts.get(1) : null);
  }
}
