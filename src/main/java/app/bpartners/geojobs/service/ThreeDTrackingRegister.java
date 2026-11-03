package app.bpartners.geojobs.service;

import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeDTrackingRegister {
  private final AnalysisProjectApi analysisProjectApi;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final CityJSONRequestRepository cityJSONRequestRepository;

  public void accept(String requestId, String communityOwnerId) {
    var request =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(requestId, communityOwnerId)
            .orElseThrow(() -> new NotFoundException("3D request not found for id=" + requestId));
    if (request.isDashboardRegistrationCompleted()) {
      log.info(
          "3D request {} already registered in dashboard {}",
          request.getId(),
          request.getDashboardRegistrationDatetime());
      return;
    }
    var community = communityAuthorizationRepository.findById(communityOwnerId).orElseThrow();
    if (!isBilledOnDashboard(community)) {
      return;
    }

    try {
      analysisProjectApi.register3d(
          community.getDashboardApiKey(), projectKeyOf(request), new Register3d(null));
    } catch (HttpClientErrorException e) {
      if (!HttpStatus.PAYMENT_REQUIRED.equals(e.getStatusCode())) {
        throw e;
      }
      log.error(
          "3D request {} delivered but not billed, insufficient credits on dashboard",
          request.getId());
      return;
    }
    cityJSONRequestRepository.markDashboardRegistered(request.getId(), now());
  }

  public static String projectKeyOf(CityJSONRequest request) {
    return AnalysisProjectKeys.of(request.getRequestDelimitations())
        .orElse("city-json:" + request.getId());
  }

  public static boolean isBilledOnDashboard(CommunityAuthorization community) {
    return !community.isIntegrationTestUsage()
        && community.getDashboardApiKey() != null
        && !community.getDashboardApiKey().isBlank();
  }
}
