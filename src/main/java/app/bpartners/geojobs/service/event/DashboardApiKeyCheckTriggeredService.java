package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@Slf4j
public class DashboardApiKeyCheckTriggeredService
    implements Consumer<DashboardApiKeyCheckTriggered> {
  private static final String FAILURE_LOG_PREFIX = "[DAKC F] ";
  private static final String SUCCESS_LOG_PREFIX = "[DAKC S] ";
  private static final String HANDLER_LOG_PREFIX = "[DAKC H] ";
  private static final String DASHBOARD_API_KEY_VERIFICATION_TEMPLATE =
      "dashboard_api_key_verification_template";
  public static final String EUROPE_PARIS = "Europe/Paris";
  public static final String EMAIL_NOTIFICATION_RECEIVER = "tech@birdia.fr";
  private final UserAccountsApi userAccountsApi;
  private final String adminApiKey;
  private final Mailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  public DashboardApiKeyCheckTriggeredService(
      UserAccountsApi userAccountsApi,
      @Value("${admin.api.key}") String adminApiKey,
      Mailer mailer,
      HTMLTemplateParser htmlTemplateParser,
      CommunityAuthorizationRepository communityAuthorizationRepository) {
    this.userAccountsApi = userAccountsApi;
    this.adminApiKey = adminApiKey;
    this.mailer = mailer;
    this.htmlTemplateParser = htmlTemplateParser;
    this.communityAuthorizationRepository = communityAuthorizationRepository;
  }

  @Override
  public void accept(DashboardApiKeyCheckTriggered event) {
    var userEmail = event.getEmail();
    var userId = event.getCommunityAuthorizationId();
    List<String> collectedErrors = new ArrayList<>();

    var retrievedUsers = userAccountsApi.getUsersByCriteria(userEmail, null, null, adminApiKey);
    var retrievedUserIds = retrievedUsers.stream().map(User::id).toList();

    if (retrievedUsers.isEmpty()) {
      var logMessage = "No users with same email as " + userId + " found in user account api.";
      log.warn(FAILURE_LOG_PREFIX + "{}", logMessage);
      return;
    }

    if (retrievedUsers.size() > 1) {
      log.warn(
          "Multiple ({}) account ( {} ) attached to the email of the user {}",
          retrievedUsers.size(),
          String.join(" ", retrievedUserIds),
          userId);
    }

    var userApiKeys =
        retrievedUsers.stream()
            .map(
                user -> {
                  try {
                    return userAccountsApi.getUserApiKey(user.id(), adminApiKey);
                  } catch (RestClientResponseException e) {
                    var exceptionMessage =
                        String.format(
                            "Unable to get api key for user with id : %s in user account api. %s"
                                + " %s",
                            user.id(), e.getStatusCode(), e.getMessage());
                    log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage, e);
                    collectedErrors.add(exceptionMessage);
                    return new ArrayList<UserApiKey>();
                  }
                })
            .flatMap(List::stream)
            .toList();

    if (userApiKeys.isEmpty()) {
      var exceptionMessage = "No api key found for users : " + retrievedUserIds;
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
      handleNoKey(userId, userEmail);
      return;
    }

    List<String> dashboardApiKeys =
        userApiKeys.stream()
            .filter(apiKey -> DASHBOARD.equals(apiKey.type()))
            .map(UserApiKey::key)
            .toList();

    if (dashboardApiKeys.isEmpty()) {
      var exceptionMessage =
          "No dashboard api key found for users : "
              + retrievedUserIds
              + " in the user account api.";
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
      handleNoKey(userId, userEmail);
      return;
    }

    String actualDashboardApiKey = event.getDashboardApiKey();
    if (!dashboardApiKeys.contains(actualDashboardApiKey)) {
      var exceptionMessage =
          "Actual dashboard api "
              + actualDashboardApiKey
              + " key doesn't match any of the api keys in the users account api for : "
              + formatIdList(retrievedUserIds);
      log.error(FAILURE_LOG_PREFIX + "{}", exceptionMessage);
      updateCommunityAuthorizationDashboardApiKey(userId, dashboardApiKeys.getFirst());
    }

    if (collectedErrors.isEmpty()) {
      log.info(
          SUCCESS_LOG_PREFIX + "Dashboard api key verification for user {} succeeded.", userId);
    }
  }

  private void handleNoKey(String communityAuthorizationEmail, String communityAuthorizationId) {
    var newDashboardApiKey = UUID.randomUUID().toString();

    var savedDashboardApiKey =
        userAccountsApi.getOrGenerateApiKey(
            communityAuthorizationEmail, newDashboardApiKey, adminApiKey);
    updateCommunityAuthorizationDashboardApiKey(
        communityAuthorizationId, savedDashboardApiKey.key());

    if (newDashboardApiKey.equals(savedDashboardApiKey.key())) {
      log.info(
          HANDLER_LOG_PREFIX + "New Dashboard api key generated for user {}",
          communityAuthorizationId);
    } else {
      log.info(
          HANDLER_LOG_PREFIX
              + "Dashboard api key for user {} already exists in the user account api. No new key"
              + " generated.",
          communityAuthorizationId);
    }
  }

  private void updateCommunityAuthorizationDashboardApiKey(
      String communityAuthorizationId, String dashboardApiKey) {
    var communityAuthorization =
        communityAuthorizationRepository.findById(communityAuthorizationId);

    if (communityAuthorization.isEmpty()) {
      log.error(
          HANDLER_LOG_PREFIX
              + "Failed to update dashboard api key for user {}. User not found in database.",
          communityAuthorizationId);
      return;
    }

    var auth = communityAuthorization.get();
    auth.setDashboardApiKey(dashboardApiKey);
    communityAuthorizationRepository.save(auth);
    log.info(
        HANDLER_LOG_PREFIX + "Dashboard api key updated for user {}", communityAuthorizationId);
  }

  @NotNull
  private static String formatIdList(List<String> idList) {
    return String.join("/", idList);
  }
}
