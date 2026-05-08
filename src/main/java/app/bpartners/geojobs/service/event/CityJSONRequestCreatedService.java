package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ROOF_SEGMENT_FACE_DELIMITATION;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStep.*;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.event.model.ThreeDRequestMonitoringTriggered;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.*;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.cityjson.texture.model.RasterInfo;
import app.bpartners.geojobs.service.lidar.LasRoofsPointsExtractor;
import app.bpartners.geojobs.service.lidar.PointsExtractionResult;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONRequestCreatedService implements Consumer<CityJSONRequestCreated> {
  private final CityJSONRequestRepository cityJSONRequestRepository;
  private final LasRoofsPointsExtractor lasRoofsPointsExtractor;
  private final LidarDataToCityJsonProcessor cityJsonProcessor;
  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final BucketComponent bucketComponent;
  private final EventProducer eventProducer;
  private final CommunityAuthorizationRepository communityAuthorizationRepository;

  @Override
  public void accept(CityJSONRequestCreated created) {
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

    try {
      var requestDelimitations = getRequestDelimitations(request);
      var pointsExtractionResult =
          lasRoofsPointsExtractor.apply(getType(request), toGeometries(requestDelimitations));

      if (isUnavailable(pointsExtractionResult)) {
        updateStatus(request, UNAVAILABLE, POINTS_CLOUD_PRE_PROCESSING);
        return;
      }

      var cityJson = toCityJSON(request, pointsExtractionResult);

      var updated =
          request.toBuilder()
              .status(FINISHED)
              .step(GEOMETRY_CONSTRUCTION)
              .cityJsons(List.of(cityJson))
              .build();
      entityManager.clear();
      cityJSONRequestRepository.save(updated);
    } catch (Exception e) {
      log.error(e.getMessage());
      updateStatus(request, FAILED, GEOMETRY_CONSTRUCTION);
      throw e;
    }
  }

  private List<Feature> getRequestDelimitations(CityJSONRequest request) {
    if (request.getFeaturesWithDelimitation() != null
        && !request.getFeaturesWithDelimitation().isEmpty()) {
      return request.getFeaturesWithDelimitation().stream()
          .map(FeatureWithDelimitation::delimitations)
          .flatMap(List::stream)
          .toList();
    }
    return request.getDelimitations();
  }

  private static boolean isUnavailable(PointsExtractionResult result) {
    if (result.data().isEmpty()) {
      return true;
    }
    return result.data().values().stream().anyMatch(data -> data.getPoints().isEmpty());
  }

  private CityJSON toCityJSON(CityJSONRequest request, PointsExtractionResult result) {
    var filename = String.format("%s.json", request.getId());
    var fileKey = String.format("city_jsons/%s", filename);
    var file = cityJsonProcessor.apply(filename, result);

    if (request.getTextures() != null && !request.getTextures().isEmpty()) {
      CityJSONTexture texture = request.getTextures().getFirst();

      Instant cityJsonTexturationStart = now();
      CityJsonTextureComputer textureComputer = new CityJsonTextureComputer(bucketComponent);
      file = textureComputer.textureCityJson(file, RasterInfo.of(texture), texture.getImageUri());
      log.info("CityJSON texturing took {} ms", now().toEpochMilli() - cityJsonTexturationStart.toEpochMilli());
    }
    bucketComponent.upload(file, fileKey);

    return CityJSON.builder().id(filename).request(request).s3FileKey(fileKey).build();
  }

  private Set<Geometry> toGeometries(List<Feature> delimitations) {
    return delimitations.stream()
        .map(featureMapper::domainToGeometryWithMultipolygonHandler)
        .collect(toSet());
  }

  private void updateStatus(
      CityJSONRequest request, CityJSONRequestStatus status, CityJSONRequestStep step) {
    var updatedStatus = request.toBuilder().status(status).step(step).build();
    entityManager.clear();
    cityJSONRequestRepository.save(updatedStatus);
  }

  private LasRoofDelimitationType getType(CityJSONRequest request) {
    return switch (request.getDelimitationObjectType()) {
      case BUILDING_ROOF_SEGMENT_FACE -> ROOF_SEGMENT_FACE_DELIMITATION;
      case null, default -> LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;
    };
  }
}
