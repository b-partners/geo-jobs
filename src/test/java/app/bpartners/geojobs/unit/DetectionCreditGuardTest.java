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
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.DetectionCreditGuard;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DetectionCreditGuardTest {
  static final String SQUARE =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.0,48.0],[2.001,48.0],[2.001,48.001],[2.0,48.001],[2.0,48.0]]]}";
  static final String OTHER_SQUARE =
      "{\"type\":\"Polygon\",\"coordinates\":"
          + "[[[2.01,48.0],[2.011,48.0],[2.011,48.001],[2.01,48.001],[2.01,48.0]]]}";

  AnalysisProjectApi analysisProjectApiMock = mock();
  CommunityAuthorizationRepository communityAuthorizationRepositoryMock = mock();
  DetectionCreditGuard subject =
      new DetectionCreditGuard(
          new AnalysisProjectBilling(analysisProjectApiMock, communityAuthorizationRepositoryMock));

  final String communityOwnerId = randomUUID().toString();
  final String detectionIdentifier = randomUUID().toString();
  final String dashboardApiKey = "dashboardApiKey";

  @Test
  void accept_individual_detection_when_credits_are_sufficient() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(eq(dashboardApiKey), eq(squareKey())))
        .thenReturn(project(true));

    assertDoesNotThrow(
        () -> subject.accept(communityOwnerId, detectionIdentifier, List.of(feature(SQUARE))));
  }

  @Test
  void refuse_individual_detection_when_credits_are_insufficient() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(eq(dashboardApiKey), eq(squareKey())))
        .thenReturn(project(false));

    assertThrows(
        PaymentRequiredException.class,
        () -> subject.accept(communityOwnerId, detectionIdentifier, List.of(feature(SQUARE))));
  }

  @Test
  void accept_individual_detection_when_dashboard_cannot_be_reached() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(
        () -> subject.accept(communityOwnerId, detectionIdentifier, List.of(feature(SQUARE))));
  }

  @Test
  void empty_zone_is_checked_with_a_detection_key_and_refused_when_credits_are_insufficient() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(eq(dashboardApiKey), eq("detection:" + detectionIdentifier)))
        .thenReturn(project(false));

    assertThrows(
        PaymentRequiredException.class,
        () -> subject.accept(communityOwnerId, detectionIdentifier, List.of()));
  }

  @Test
  void several_geometries_are_checked_with_a_detection_key() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(eq(dashboardApiKey), eq("detection:" + detectionIdentifier)))
        .thenReturn(project(true));

    assertDoesNotThrow(
        () ->
            subject.accept(
                communityOwnerId,
                detectionIdentifier,
                List.of(feature(SQUARE), feature(OTHER_SQUARE))));
  }

  @Test
  void empty_zone_is_accepted_when_dashboard_cannot_be_reached() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(() -> subject.accept(communityOwnerId, detectionIdentifier, List.of()));
  }

  @Test
  void several_geometries_are_accepted_when_dashboard_cannot_be_reached() {
    givenBilledCommunity();
    when(analysisProjectApiMock.get(any(), any())).thenThrow(new RuntimeException("down"));

    assertDoesNotThrow(
        () ->
            subject.accept(
                communityOwnerId,
                detectionIdentifier,
                List.of(feature(SQUARE), feature(OTHER_SQUARE))));
  }

  @Test
  void skip_community_not_billed_on_dashboard() {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(Optional.of(CommunityAuthorization.builder().build()));

    assertDoesNotThrow(
        () -> subject.accept(communityOwnerId, detectionIdentifier, List.of(feature(SQUARE))));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  @Test
  void skip_detection_without_community_owner() {
    assertDoesNotThrow(() -> subject.accept(null, detectionIdentifier, List.of(feature(SQUARE))));

    verify(analysisProjectApiMock, never()).get(any(), any());
  }

  private void givenBilledCommunity() {
    when(communityAuthorizationRepositoryMock.findById(communityOwnerId))
        .thenReturn(
            Optional.of(CommunityAuthorization.builder().dashboardApiKey(dashboardApiKey).build()));
  }

  private static String squareKey() {
    return AnalysisProjectKeys.of(List.of(feature(SQUARE))).orElseThrow();
  }

  private static Feature feature(String geometryJson) {
    return Feature.builder()
        .geometry(Feature.FeatureGeometry.builder().actualInstanceStringValue(geometryJson).build())
        .build();
  }

  private static AnalysisProject project(boolean creditsSufficient) {
    return new AnalysisProject(
        "key", false, false, false, List.of("ANALYSIS_2D", "MODEL_3D"), 1L, creditsSufficient);
  }
}
