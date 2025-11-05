package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus.*;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.model.CityJSONRequestCreated;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.CityJSONRequestRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequestStatus;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor.RoofsAnalysisResult;
import jakarta.persistence.EntityManager;
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
  private final LidarRoofsAnalysisProcessor lidarProcessor;
  private final LidarDataToCityJsonProcessor cityJsonProcessor;
  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(CityJSONRequestCreated created) {
    var request = cityJSONRequestRepository.findById(created.getRequestId()).orElseThrow();
    if (!request.canBeProcessed()) {
      return;
    }

    try {
      updateStatus(request, PROCESSING);

      var lidarAnalysisResult = lidarProcessor.apply(toGeometries(request.getDelimitations()));
      var cityJson = toCityJSON(request, lidarAnalysisResult);

      var updated = request.toBuilder().status(FINISHED).cityJsons(List.of(cityJson)).build();
      entityManager.clear();
      cityJSONRequestRepository.save(updated);
    } catch (Exception e) {
      log.error(e.getMessage());
      updateStatus(request, FAILED);
    }
  }

  private CityJSON toCityJSON(CityJSONRequest request, RoofsAnalysisResult lidarAnalysisResult) {
    var file = cityJsonProcessor.apply(lidarAnalysisResult);
    var id = randomUUID().toString();
    var fileKey = String.format("city_jsons/%s_%s.json", request.getId(), id);

    bucketComponent.upload(file, fileKey);
    return CityJSON.builder().id(id).s3FileKey(fileKey).build();
  }

  private Set<Geometry> toGeometries(List<Feature> delimitations) {
    return delimitations.stream().map(featureMapper::domainToGeometry).collect(toSet());
  }

  private void updateStatus(CityJSONRequest request, CityJSONRequestStatus status) {
    var updatedStatus = request.toBuilder().status(status).build();
    entityManager.clear();
    cityJSONRequestRepository.save(updatedStatus);
  }
}
