package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DashboardApiKeyCheckTriggeredService
    implements Consumer<DashboardApiKeyCheckTriggered> {
  private final UserAccountsApi userAccountsApi;
  private final String adminApiKey;

  public DashboardApiKeyCheckTriggeredService(
      UserAccountsApi userAccountsApi, @Value("${admin.api.key}") String adminApiKey) {
    this.userAccountsApi = userAccountsApi;
    this.adminApiKey = adminApiKey;
  }

  @Override
  public void accept(DashboardApiKeyCheckTriggered event) {
    try {
      CommunityAuthorization authorization = event.getCommunityAuthorization();
      checkDashboardApiKeyValidity(authorization);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void checkDashboardApiKeyValidity(CommunityAuthorization authorization) {
    List<User> retrievedUsers =
        userAccountsApi.getUsersByCriteria(authorization.getEmail(), null, null, adminApiKey);

    if (retrievedUsers.isEmpty()) {
      throw new NotFoundException(
          "Users with email " + authorization.getEmail() + " not found in user accounts api.");
    }

    if (retrievedUsers.size() > 1) {
      log.warn(
          "Multiple ({}) account attached to the email : {} in user account api",
          retrievedUsers.size(),
          authorization.getEmail());
    }

    User distinctUser = retrievedUsers.getFirst();
    List<UserApiKey> userApiKeys = userAccountsApi.getUserApiKey(distinctUser.id(), adminApiKey);

    if (userApiKeys.isEmpty()) {
      throw new NotFoundException("No api found for user with email : " + distinctUser.email());
    }

    List<String> dashboardApiKeys =
        userApiKeys.stream()
            .filter(apiKey -> DASHBOARD.equals(apiKey.type()))
            .map(UserApiKey::key)
            .toList();

    if (dashboardApiKeys.isEmpty()) {
      throw new NotFoundException(
          "No dashboard api key found for user with email : "
              + distinctUser.email()
              + "in the user account api.");
    }

    String actualDashboardApiKey = authorization.getDashboardApiKey();
    if (!dashboardApiKeys.contains(actualDashboardApiKey)) {
      throw new NotFoundException(
          "Actual dashboard api "
              + actualDashboardApiKey
              + " key doesn't match any of the api keys in the user account api for : "
              + distinctUser.email());
    }
  }
}
