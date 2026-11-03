package app.bpartners.geojobs.service;

import static java.time.Instant.now;

import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.community.CommunityAuthorization;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.dashboard.AnalysisProjectApi;
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
    analysisProjectApi.register2d(
        getApiKey(detection),
        projectKeyOf(detection),
        new Register2d(
            detection.getEndToEndId(),
            detection.getZoneName(),
            "non supportée",
            now(),
            new DetectionInitiator("non supporté", detection.getEmailReceiver(), "non supporté")));

    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

    detectionRepository.save(
        actualDetection.toBuilder().dashboardRegistrationDatetime(now()).build());
  }

  private String projectKeyOf(Detection detection) {
    var detectionKey = "detection:" + detection.getId();
    var convertedAddresses = detection.getConvertedAddresses();
    if (convertedAddresses != null && convertedAddresses.size() > 1) {
      return detectionKey;
    }
    return AnalysisProjectKeys.ofIndividual(detection.getDomainProvidedGeoJsonZone())
        .orElse(detectionKey);
  }

  private String getApiKey(Detection detection) {
    return communityAuthorizationRepository
        .findById(detection.getCommunityOwnerId())
        .map(CommunityAuthorization::getDashboardApiKey)
        .orElseThrow();
  }
}
