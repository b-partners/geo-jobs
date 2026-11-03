package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.PaymentRequiredException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.ThreeDCreditGuard;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThreeDCreditGuardTest {
  AnalysisProjectApi analysisProjectApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  ThreeDCreditGuard subject =
      new ThreeDCreditGuard(analysisProjectApiMock, communityAuthorizationRepositoryMock);

  final String communityOwnerId = randomUUID().toString();
  final String dashboardApiKey = "dashboardApiKey";
  final CityJSONRequest request =
      CityJSONRequest.builder()
          .id(randomUUID().toString())
          .communityOwnerId(communityOwnerId)
          .delimitations(List.of())
          .build();

  @Test
  void accept_when_credits_are_sufficient() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(eq(dashboardApiKey), eq("city-json:" + request.getId())))
        .thenReturn(project(true));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void refuse_when_credits_are_insufficient() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenReturn(project(false));

    assertThrows(PaymentRequiredException.class, () -> subject.accept(request));
  }

  @Test
  void accept_when_dashboard_cannot_be_reached() {
    givenCommunity(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build());
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(() -> subject.accept(request));
  }

  @Test
  void skip_community_not_billed_on_dashboard() {
    givenCommunity(CommunityAuthorization.builder().build());

    assertDoesNotThrow(() -> subject.accept(request));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  @Test
  void skip_request_without_community_owner() {
    var withoutOwner = request.toBuilder().communityOwnerId(null).build();

    assertDoesNotThrow(() -> subject.accept(withoutOwner));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  private void givenCommunity(CommunityAuthorization community) {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(community));
  }

  private static AnalysisProject project(boolean creditsSufficient) {
    return new AnalysisProject(
        "key", false, false, false, List.of("ANALYSIS_2D", "MODEL_3D"), 1L, creditsSufficient);
  }
}
