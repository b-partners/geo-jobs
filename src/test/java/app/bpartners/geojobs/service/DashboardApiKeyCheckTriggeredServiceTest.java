package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.ANALYSIS;
import static app.bpartners.geojobs.service.dashboard.component.UserApiKeyType.DASHBOARD;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DashboardApiKeyCheckTriggered;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.UserAccountsApi;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardApiKeyCheckTriggeredServiceTest {

  UserAccountsApi userAccountsApiMock = mock(UserAccountsApi.class);
  DashboardApiKeyCheckTriggeredService subject;

  @Test
  void check_on_non_existant_user_throws_exception() {
    CommunityAuthorization authorizationMock = mock();
    String adminApiKey = randomUUID().toString();

    when(authorizationMock.getEmail()).thenReturn("non-existant-email");
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorizationMock.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of());
    subject = new DashboardApiKeyCheckTriggeredService(userAccountsApiMock, adminApiKey);
    DashboardApiKeyCheckTriggered event = new DashboardApiKeyCheckTriggered(authorizationMock);

    var actual = assertThrows(NotFoundException.class, () -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorizationMock.getEmail()), eq(null), eq(null), eq(adminApiKey));
    assertEquals(
        "Users with email " + authorizationMock.getEmail() + " not found in user accounts api.",
        actual.getMessage());
  }

  @Test
  void no_keys_found_on_existing_user_throws_exception() {
    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            "existant@mail.com");
    String adminApiKey = randomUUID().toString();

    when(authorization.getEmail()).thenReturn("existant-email");
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey))).thenReturn(List.of());
    subject = new DashboardApiKeyCheckTriggeredService(userAccountsApiMock, adminApiKey);
    DashboardApiKeyCheckTriggered event = new DashboardApiKeyCheckTriggered(authorization);

    var actual = assertThrows(NotFoundException.class, () -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    assertEquals("No api found for user with email : " + user.email(), actual.getMessage());
  }

  @Test
  void no_dashboard_keys_found_on_existing_user_throws_exception() {
    String adminApiKey = randomUUID().toString();
    String existingEmail = "exist@" + randomUUID();

    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            existingEmail);

    when(authorization.getEmail()).thenReturn(existingEmail);
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey)))
        .thenReturn(List.of(new UserApiKey(randomUUID().toString(), ANALYSIS)));
    subject = new DashboardApiKeyCheckTriggeredService(userAccountsApiMock, adminApiKey);
    DashboardApiKeyCheckTriggered event = new DashboardApiKeyCheckTriggered(authorization);

    var actual = assertThrows(NotFoundException.class, () -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    verify(userAccountsApiMock, times(1)).getUserApiKey(eq(user.id()), eq(adminApiKey));
    assertEquals(
        "No dashboard api key found for user with email : "
            + user.email()
            + "in the user account api.",
        actual.getMessage());
  }

  @Test
  void no_dashboard_keys_matches_actual_keys_throws_exception() {
    String adminApiKey = randomUUID().toString();
    String actualDashboardApiKey = randomUUID().toString();
    String existingEmail = "exist@" + randomUUID();

    CommunityAuthorization authorization = mock();
    User user =
        new User(
            randomUUID().toString(),
            randomUUID().toString(),
            randomUUID().toString(),
            existingEmail);

    when(authorization.getEmail()).thenReturn(existingEmail);
    when(authorization.getDashboardApiKey()).thenReturn(actualDashboardApiKey);
    when(userAccountsApiMock.getUsersByCriteria(
            eq(authorization.getEmail()), eq(null), eq(null), anyString()))
        .thenReturn(List.of(user));
    when(userAccountsApiMock.getUserApiKey(eq(user.id()), eq(adminApiKey)))
        .thenReturn(List.of(new UserApiKey(randomUUID().toString(), DASHBOARD)));
    subject = new DashboardApiKeyCheckTriggeredService(userAccountsApiMock, adminApiKey);
    DashboardApiKeyCheckTriggered event = new DashboardApiKeyCheckTriggered(authorization);

    var actual = assertThrows(NotFoundException.class, () -> subject.accept(event));

    verify(userAccountsApiMock, times(1))
        .getUsersByCriteria(eq(authorization.getEmail()), eq(null), eq(null), eq(adminApiKey));
    verify(userAccountsApiMock, times(1)).getUserApiKey(eq(user.id()), eq(adminApiKey));
    assertEquals(
        "Actual dashboard api "
            + actualDashboardApiKey
            + " key doesn't match any of the api keys in the user account api for : "
            + user.email(),
        actual.getMessage());
  }
}
