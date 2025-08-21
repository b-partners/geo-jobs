package app.bpartners.geojobs.service.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.service.dashboard.component.Account;
import app.bpartners.geojobs.service.dashboard.component.User;
import app.bpartners.geojobs.service.dashboard.component.UserApiKey;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UserAccountsApiTest {
  ApiConfiguration mockApiConfiguration;
  SecurityApi mockSecurityApi;
  UserAccountsApi subject;

  @BeforeEach
  void mockDependenciesSetUp() {
    mockApiConfigurationSetUp();
    mockSecurityApiSetUp();
  }

  void mockApiConfigurationSetUp() {
    mockApiConfiguration = Mockito.mock(ApiConfiguration.class);
    when(mockApiConfiguration.getDashboardApiUrl()).thenReturn("https://mocked.dashboard.api");
  }

  void mockSecurityApiSetUp() {
    mockSecurityApi = Mockito.mock(SecurityApi.class);
    when(mockSecurityApi.retrieveUserId("_user-api-key_")).thenReturn("_user-id_");
  }

  MockedConstruction<RestTemplate> mockConstructedRestTemplate() {
    return Mockito.mockConstruction(
        RestTemplate.class,
        (mock, context) -> {
          when(mock.exchange(
                  eq(
                      String.format(
                          "%s/users/%s/accounts",
                          mockApiConfiguration.getDashboardApiUrl(), "_user-id_")),
                  eq(GET),
                  argThat(
                      (HttpEntity<?> entity) ->
                          entity.getHeaders().containsKey(ApiConfiguration.API_KEY_HEADER)),
                  any(ParameterizedTypeReference.class)))
              .thenReturn(new ResponseEntity<>(List.of(getMockAccount()), HttpStatus.OK));

          when(mock.exchange(
                  eq(
                      URI.create(
                          String.format(
                              "%s/users?email=%s",
                              mockApiConfiguration.getDashboardApiUrl(), "_user-email_"))),
                  eq(GET),
                  argThat(
                      (HttpEntity<?> entity) ->
                          entity.getHeaders().containsKey(ApiConfiguration.API_KEY_HEADER)),
                  any(ParameterizedTypeReference.class)))
              .thenReturn(new ResponseEntity<>(List.of(getMockUser()), HttpStatus.OK));

          when(mock.exchange(
                  argThat(
                      (URI uri) ->
                          !uri.equals(
                              URI.create(
                                  String.format(
                                      "%s/users?email=%s",
                                      mockApiConfiguration.getDashboardApiUrl(), "_user-email_")))),
                  eq(GET),
                  argThat(
                      (HttpEntity<?> entity) ->
                          entity.getHeaders().containsKey(ApiConfiguration.API_KEY_HEADER)),
                  any(ParameterizedTypeReference.class)))
              .thenReturn(ResponseEntity.badRequest().body(List.of()));

          when(mock.exchange(
                  eq(
                      String.format(
                          "%s/users/%s/keys",
                          mockApiConfiguration.getDashboardApiUrl(), "_user-id_")),
                  eq(POST),
                  argThat(
                      (HttpEntity<?> entity) ->
                          entity.getHeaders().containsKey(ApiConfiguration.API_KEY_HEADER)
                              && entity
                                  .getHeaders()
                                  .getFirst(ApiConfiguration.API_KEY_HEADER)
                                  .equals("_admin-api-key_")
                              && entity.getBody() instanceof UserApiKey),
                  any(ParameterizedTypeReference.class)))
              .thenReturn(new ResponseEntity(new UserApiKey("_new-user-api-key_"), HttpStatus.OK));

          when(mock.exchange(
                  argThat((String s) -> !s.matches(".*/users/_user-id_/keys")),
                  eq(POST),
                  argThat(
                      (HttpEntity<?> entity) ->
                          !entity.getHeaders().containsKey(ApiConfiguration.API_KEY_HEADER)
                              || !entity
                                  .getHeaders()
                                  .getFirst(ApiConfiguration.API_KEY_HEADER)
                                  .equals("_admin-api-key_")),
                  any(ParameterizedTypeReference.class)))
              .thenReturn(ResponseEntity.badRequest().build());
        });
  }

  Account getMockAccount() {
    return new Account("_account-id_", "_account-name_", true);
  }

  User getMockUser() {
    return new User("_user-id_", "_user-name_", "_user-surname_");
  }

  @Test
  void get_accounts_by_user_id() {
    try (MockedConstruction<RestTemplate> mocked = mockConstructedRestTemplate()) {
      subject = new UserAccountsApi(mockApiConfiguration, mockSecurityApi);

      var expected = List.of(getMockAccount());
      var actual = subject.getAccountsByUserId("_user-id_", "_user-api-key_");

      assertEquals(expected, actual);
    }
  }

  @Test
  void get_active_by_user_id() {
    try (MockedConstruction<RestTemplate> mocked = mockConstructedRestTemplate()) {
      subject = new UserAccountsApi(mockApiConfiguration, mockSecurityApi);

      var actual = subject.getActiveByUserId("_user-api-key_");

      assertEquals(getMockAccount(), actual);
    }
  }

  @Test
  void get_users_by_criteria() {
    try (MockedConstruction<RestTemplate> mocked = mockConstructedRestTemplate()) {
      subject = new UserAccountsApi(mockApiConfiguration, mockSecurityApi);

      var actual = subject.getUsersByCriteria("_user-email_", 1, 500, "_user-api-key_");

      assertEquals(1, actual.size());
      assertEquals(getMockUser(), actual.getFirst());
    }
  }

  @Test
  void update_api_key() {
    try (MockedConstruction<RestTemplate> mocked = mockConstructedRestTemplate()) {
      subject = new UserAccountsApi(mockApiConfiguration, mockSecurityApi);

      var actual = subject.updateApiKey("_user-email_", "_new-user-api-key_", "_admin-api-key_");

      assertEquals("_new-user-api-key_", actual.key());
      assertThrows(
          NotFoundException.class,
          () -> {
            subject.updateApiKey("_404-email_", "_new-user-api-key_", "_admin-api-key_");
          });
    }
  }
}
