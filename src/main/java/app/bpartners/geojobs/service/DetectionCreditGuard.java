package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionCreditGuard {
  private final AnalysisProjectBilling analysisProjectBilling;

  public void accept(
      String communityOwnerId, String detectionIdentifier, List<Feature> providedFeatures) {
    var individual = AnalysisProjectKeys.isIndividual(providedFeatures);
    analysisProjectBilling.checkCredits(
        communityOwnerId,
        projectKeyOf(detectionIdentifier, providedFeatures, individual),
        !isProvenBulk(providedFeatures),
        "detection " + detectionIdentifier,
        "run this detection");
  }

  private static boolean isProvenBulk(List<Feature> providedFeatures) {
    return providedFeatures != null
        && providedFeatures.stream()
                .filter(Objects::nonNull)
                .filter(feature -> feature.getGeometry() != null)
                .count()
            > 1;
  }

  private static String projectKeyOf(
      String detectionIdentifier, List<Feature> providedFeatures, boolean individual) {
    var detectionKey = "detection:" + detectionIdentifier;
    if (!individual) {
      return detectionKey;
    }
    return AnalysisProjectKeys.of(providedFeatures).orElse(detectionKey);
  }
}
