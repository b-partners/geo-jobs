package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.Register3d;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeDTrackingRegister {
  private final AnalysisProjectApi analysisProjectApi;
  private final AnalysisProjectBilling analysisProjectBilling;
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
    var registered =
        analysisProjectBilling.register(
            community,
            "3D request " + request.getId(),
            apiKey ->
                analysisProjectApi.register3d(apiKey, projectKeyOf(request), new Register3d(null)));
    if (registered) {
      cityJSONRequestRepository.markDashboardRegistered(request.getId(), now());
    }
  }

  public static boolean isIndividual(CityJSONRequest request) {
    var delimitations = request.getRequestDelimitations();
    if (BUILDING_ROOF_SEGMENT_FACE.equals(request.getDelimitationObjectType())) {
      return AnalysisProjectKeys.hasGeometry(delimitations);
    }
    return AnalysisProjectKeys.isIndividual(delimitations);
  }

  public static String projectKeyOf(CityJSONRequest request) {
    var requestKey = "city-json:" + request.getId();
    if (!isIndividual(request)) {
      return requestKey;
    }
    return AnalysisProjectKeys.of(request.getRequestDelimitations()).orElse(requestKey);
  }
}
