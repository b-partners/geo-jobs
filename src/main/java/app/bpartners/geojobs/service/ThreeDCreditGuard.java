package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.ThreeDTrackingRegister.isBilledOnDashboard;
import static app.bpartners.geojobs.service.ThreeDTrackingRegister.isIndividual;
import static app.bpartners.geojobs.service.ThreeDTrackingRegister.projectKeyOf;

import app.bpartners.geojobs.model.exception.PaymentRequiredException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.component.AnalysisProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreeDCreditGuard {
  private final AnalysisProjectApi analysisProjectApi;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  public void accept(CityJSONRequest request) {
    if (request.getCommunityOwnerId() == null) {
      return;
    }
    var community =
        communityAuthorizationRepository.findById(request.getCommunityOwnerId()).orElse(null);
    if (community == null || !isBilledOnDashboard(community)) {
      return;
    }

    AnalysisProject project;
    try {
      project = analysisProjectApi.get(community.getDashboardApiKey(), projectKeyOf(request));
    } catch (RuntimeException e) {
      if (!isIndividual(request)) {
        throw e;
      }
      log.warn("Unable to check credits before 3D request {}", request.getId(), e);
      return;
    }
    if (!project.creditsSufficient()) {
      throw new PaymentRequiredException(
          "Not enough analysis credits to generate this 3D model, "
              + project.creditCost()
              + " credit(s) required. Please, buy credits on dashboard to access to this API.");
    }
  }
}
