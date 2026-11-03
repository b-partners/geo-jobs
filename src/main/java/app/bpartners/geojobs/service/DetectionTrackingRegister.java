package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.dashboard.DashboardRegistrationType.DETECTION_2D;
import static java.time.Instant.now;

import app.bpartners.geojobs.model.exception.DashboardUnavailableException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectBilling;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectKeys;
import app.bpartners.geojobs.service.dashboard.component.DetectionInitiator;
import app.bpartners.geojobs.service.dashboard.component.Register2d;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionTrackingRegister implements Consumer<Detection> {
  private final AnalysisProjectApi analysisProjectApi;
  private final AnalysisProjectBilling analysisProjectBilling;
  private final DashboardRegistrationFailureService dashboardRegistrationFailureService;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(Detection detection) {
    if (detection.isDashboardRegistrationCompleted()) {
      log.info(
          "Detection {} already registered in dashboard {}",
          detection.getId(),
          detection.getDashboardRegistrationDatetime());
      return;
    }
    var community =
        communityAuthorizationRepository.findById(detection.getCommunityOwnerId()).orElseThrow();
    var projectKey = projectKeyOf(detection);
    var register2d =
        new Register2d(
            detection.getEndToEndId(),
            detection.getZoneName(),
            "non supportée",
            now(),
            new DetectionInitiator("non supporté", detection.getEmailReceiver(), "non supporté"));
    boolean registered;
    try {
      registered =
          analysisProjectBilling.register(
              community,
              "Detection " + detection.getId(),
              apiKey -> analysisProjectApi.register2d(apiKey, projectKey, register2d));
    } catch (DashboardUnavailableException e) {
      dashboardRegistrationFailureService.record(
          DETECTION_2D,
          detection.getId(),
          detection.getCommunityOwnerId(),
          projectKey,
          register2d,
          e);
      return;
    }
    dashboardRegistrationFailureService.markReprocessed(DETECTION_2D, detection.getId());
    if (!registered) {
      return;
    }

    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

    detectionRepository.save(
        actualDetection.toBuilder().dashboardRegistrationDatetime(now()).build());
  }

  public static boolean isIndividual(Detection detection) {
    var convertedAddresses = detection.getConvertedAddresses();
    if (convertedAddresses != null && convertedAddresses.size() > 1) {
      return false;
    }
    return AnalysisProjectKeys.isIndividual(detection.getDomainProvidedGeoJsonZone());
  }

  public static String projectKeyOf(Detection detection) {
    var detectionKey = "detection:" + detection.getId();
    if (!isIndividual(detection)) {
      return detectionKey;
    }
    return AnalysisProjectKeys.of(detection.getDomainProvidedGeoJsonZone()).orElse(detectionKey);
  }
}
