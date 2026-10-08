package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONDelimitationObjectType.BUILDING_ROOF_SEGMENT_FACE;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.cityjson.*;
import app.bpartners.geojobs.service.CityJSON3DBagRooferProcessor;
import app.bpartners.geojobs.service.CityJSONSafeModeProcessor;
import app.bpartners.geojobs.service.CityJSONThreedProcessor;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONRequestCreatedService implements Consumer<CityJSONRequestCreated> {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final EntityManager entityManager;
  private final EventProducer eventProducer;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;
  private final CityJSON3DBagRooferProcessor cityJson3DBagRooferProcessor;
  private final CityJSONSafeModeProcessor cityJSONSafeModeProcessor;
  private final CityJSONThreedProcessor cityJSONThreedProcessor;

  // TODO: refactor
  public void accept(CityJSONRequestCreated created, boolean isSync) {
    var communityAuthorization =
        communityAuthorizationRepository.findById(created.getCommunityOwnerId()).orElseThrow();
    if (!communityAuthorization.isIntegrationTestUsage()) {
      eventProducer.accept(
          List.of(
              new ThreeDRequestMonitoringTriggered(
                  created.getRequestId(), created.getCommunityOwnerId())));
    }

    var request =
        cityJSONRequestRepository
            .findByIdAndCommunityOwnerId(created.getRequestId(), created.getCommunityOwnerId())
            .orElseThrow();

    processCityJSONFacade(request, isSync);
  }

  @SneakyThrows
  @Override
  public void accept(CityJSONRequestCreated created) {
    accept(created, false);
  }

  private void succeedCityJsonRequest(CityJSONRequest request, List<CityJSON> cityJsonList) {
    var updated =
        request.toBuilder()
            .status(FINISHED)
            .step(GEOMETRY_CONSTRUCTION)
            .cityJsons(cityJsonList)
            .build();
    entityManager.clear();
    cityJSONRequestRepository.save(updated);
  }

  private void updateStatus(
      CityJSONRequest request, CityJSONRequestStatus status, CityJSONRequestStep step) {
    var updatedStatus = request.toBuilder().status(status).step(step).build();
    entityManager.clear();
    cityJSONRequestRepository.save(updatedStatus);
  }

  private void processCityJSONFacade(CityJSONRequest request, boolean isSync) {
    if (BUILDING_ROOF_SEGMENT_FACE.equals(request.getDelimitationObjectType())) {
      processByThreed(request);
      return;
    }
    processFullAutomaticFacade(request, isSync);
  }

  private void processByThreed(CityJSONRequest request) {
    try {
      var cityJsons = cityJSONThreedProcessor.apply(request);
      succeedCityJsonRequest(request, cityJsons);
    } catch (Exception e) {
      log.error(
          "Unable to process Lidar by threed for request {}, error {}",
          request.getId(),
          e.getMessage(),
          e);
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
      throw e;
    }
  }

  private void processFullAutomaticFacade(CityJSONRequest request, boolean isSync) {
    var processorType = request.getLidarProcessorType();
    switch (processorType) {
      case DEFAULT -> processByThreed(request);
      case SAFE_MODE -> processFullAutomaticBySafeMode(request);
      case null, default -> processFullAutomaticBy3DBag(request);
    }
  }

  private void processFullAutomaticBy3DBag(CityJSONRequest request) {
    try {
      var cityJsonFrom3dBag = cityJson3DBagRooferProcessor.apply(request);
      succeedCityJsonRequest(request, cityJsonFrom3dBag);
    } catch (Exception e) {
      log.error(
          "Unable to process Lidar for request {}, error {}", request.getId(), e.getMessage(), e);
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
    }
  }

  private void processFullAutomaticBySafeMode(CityJSONRequest request) {
    try {
      var cityjsonFile = cityJSONSafeModeProcessor.apply(request);
      succeedCityJsonRequest(request, cityjsonFile);
    } catch (Exception e) {
      log.error(
          "Unable to process Lidar by SafeMode for request {}, error {}",
          request.getId(),
          e.getMessage());
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
    }
  }
}
