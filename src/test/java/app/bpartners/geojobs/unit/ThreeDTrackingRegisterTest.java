package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.ThreeDTrackingRegister;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class ThreeDTrackingRegisterTest {
  static final String POLYGON =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]}";

  AnalysisProjectApi analysisProjectApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  CityJSONRequestRepository cityJSONRequestRepositoryMock = mock();
  ThreeDTrackingRegister subject =
      new ThreeDTrackingRegister(
          analysisProjectApiMock,
          communityAuthorizationRepositoryMock,
          cityJSONRequestRepositoryMock);

  final String requestId = randomUUID().toString();
  final String communityOwnerId = randomUUID().toString();
  final String dashboardApiKey = "dashboardApiKey";

  @Test
  void register_3d_with_geometry_key_and_mark_request_registered() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any())).thenReturn(mock());

    subject.accept(requestId, communityOwnerId);

    var projectKeyCaptor = ArgumentCaptor.forClass(String.class);
    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), projectKeyCaptor.capture(), eq(new Register3d(null)));
    assertEquals(
        AnalysisProjectKeys.of(List.of(polygonFeature())).orElseThrow(),
        projectKeyCaptor.getValue());
    verify(cityJSONRequestRepositoryMock).markDashboardRegistered(eq(requestId), any());
  }

  @Test
  void register_3d_falls_back_to_a_request_key_without_geometry() {
    givenRequest(requestBuilder().delimitations(List.of()).build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());

    subject.accept(requestId, communityOwnerId);

    verify(analysisProjectApiMock)
        .register3d(eq(dashboardApiKey), eq("city-json:" + requestId), any());
  }

  @Test
  void skip_request_already_registered() {
    givenRequest(requestBuilder().dashboardRegistrationDatetime(Instant.now()).build());

    subject.accept(requestId, communityOwnerId);

    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void skip_community_without_dashboard_api_key() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().build());

    subject.accept(requestId, communityOwnerId);

    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void skip_integration_test_community() {
    givenRequest(requestBuilder().build());
    givenCommunity(
        CommunityAuthorization.builder()
            .dashboardApiKey(dashboardApiKey)
            .integrationTestUsage(true)
            .build());

    subject.accept(requestId, communityOwnerId);

    verify(analysisProjectApiMock, never()).register3d(any(), any(), any());
  }

  @Test
  void insufficient_credits_are_swallowed_and_request_stays_unregistered() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(clientError(HttpStatus.PAYMENT_REQUIRED));

    assertDoesNotThrow(() -> subject.accept(requestId, communityOwnerId));

    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void other_api_failures_are_rethrown_to_be_retried() {
    givenRequest(requestBuilder().build());
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.register3d(any(), any(), any()))
        .thenThrow(clientError(HttpStatus.FORBIDDEN));

    assertThrows(HttpClientErrorException.class, () -> subject.accept(requestId, communityOwnerId));
    verify(cityJSONRequestRepositoryMock, never()).markDashboardRegistered(any(), any());
  }

  @Test
  void unknown_request_is_not_found() {
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.accept(requestId, communityOwnerId));
  }

  @Test
  void billing_applies_only_to_non_test_communities_with_a_dashboard_key() {
    assertTrue(
        ThreeDTrackingRegister.isBilledOnDashboard(
            CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build()));
    assertEquals(
        false,
        ThreeDTrackingRegister.isBilledOnDashboard(
            CommunityAuthorization.builder().dashboardApiKey(" ").build()));
  }

  private CityJSONRequest.CityJSONRequestBuilder requestBuilder() {
    return CityJSONRequest.builder()
        .id(requestId)
        .communityOwnerId(communityOwnerId)
        .delimitations(List.of(polygonFeature()));
  }

  private void givenRequest(CityJSONRequest request) {
    when(cityJSONRequestRepositoryMock.findByIdAndCommunityOwnerId(requestId, communityOwnerId))
        .thenReturn(Optional.of(request));
  }

  private void givenCommunity(CommunityAuthorization community) {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(community));
  }

  private static Feature polygonFeature() {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(POLYGON).build())
        .build();
  }

  private static HttpClientErrorException clientError(HttpStatus status) {
    return HttpClientErrorException.create(status, "error", HttpHeaders.EMPTY, new byte[0], null);
  }
}
