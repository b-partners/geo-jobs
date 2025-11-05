package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.cityjson.LidarDataToCityJsonProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor.RoofsAnalysisResult;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionCityJSONGenerator implements BiConsumer<Detection, RoofsAnalysisResult> {
  private final LidarDataToCityJsonProcessor lidarDataToCityJsonProcessor;
  private final EntityManager entityManager;
  private final FeatureMapper featureMapper;
  private final DetectionRepository detectionRepository;
  private final BucketComponent bucketComponent;

  @Override
  public void accept(Detection detection, RoofsAnalysisResult roofsAnalysisResult) {
    var delimitations = detection.getFeatureWithDelimitations();
    var cityJSONFiles =
        delimitations.parallelStream()
            .map(
                featureWithDelimitation ->
                    retrieveCityJSON(roofsAnalysisResult, featureWithDelimitation))
            .collect(toSet());
    var cityJSONModels = uploadAndMapToCityJSONModel(detection, cityJSONFiles);

    entityManager.clear();
    var actualDetection = detectionRepository.findById(detection.getId()).orElseThrow();

    detectionRepository.save(actualDetection.toBuilder().cityJsons(cityJSONModels).build());
  }

  private List<CityJSON> uploadAndMapToCityJSONModel(Detection detection, Set<File> cityJSONFiles) {
    return cityJSONFiles.parallelStream()
        .map(
            file -> {
              var id = randomUUID().toString();
              var key = String.format("cityjson/detection_%s_%s.json", detection.getId(), id);
              bucketComponent.upload(file, key);

              return CityJSON.builder()
                  .id(id)
                  .s3FileKey(key)
                  .detection(detection)
                  .creationDatetime(now())
                  .build();
            })
        .toList();
  }

  private File retrieveCityJSON(
      RoofsAnalysisResult roofsAnalysisResult, FeatureWithDelimitation delimitation) {
    List<Feature> delimitations =
        delimitation.getRestDelimitations() == null
            ? List.of()
            : delimitation.getRestDelimitations();
    var roofsData =
        delimitations.stream()
            .map(
                subDelimitation -> {
                  var domainGeometry = featureMapper.toDomainGeometry(subDelimitation);
                  return roofsAnalysisResult.getData(domainGeometry);
                })
            .collect(toSet());

    return lidarDataToCityJsonProcessor.apply(roofsData);
  }
}
