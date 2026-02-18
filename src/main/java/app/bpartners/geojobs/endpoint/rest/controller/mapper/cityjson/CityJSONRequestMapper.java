package app.bpartners.geojobs.endpoint.rest.controller.mapper.cityjson;

import static java.time.Instant.now;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityJSONRequestMapper {
  private final BucketComponent bucketComponent;

  public CityJSONRequest toRest(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest cityJSONRequest) {
    var restDelimitations =
        cityJSONRequest.getDelimitations().stream().map(FeatureMapper::toRestFeature).toList();

    List<CityJSON> cityJsons =
        cityJSONRequest.getCityJsons() == null ? List.of() : cityJSONRequest.getCityJsons();
    var restCityJsons =
        cityJsons.parallelStream()
            .map(
                cityJson -> {
                  var fileUrl = bucketComponent.presign(cityJson.getS3FileKey());
                  return CityJSONMapper.toRest(cityJson, fileUrl);
                })
            .toList();

    return new CityJSONRequest()
        .id(cityJSONRequest.getId())
        .delimitations(restDelimitations)
        .status(CityJSONRequestStatusMapper.toRest(cityJSONRequest.getStatus()))
        .cityJsons(restCityJsons);
  }

  public ThreeDResponseStatus toRestThreeDResponseStatus(
      app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest cityJSONRequest) {
    var restDelimitations =
        cityJSONRequest.getDelimitations().stream().map(FeatureMapper::toRestFeature).toList();

    List<CityJSON> cityJsons =
        cityJSONRequest.getCityJsons() == null ? List.of() : cityJSONRequest.getCityJsons();
    var restCityJsons =
        cityJsons.parallelStream()
            .map(
                cityJson -> {
                  var fileUrl = bucketComponent.presign(cityJson.getS3FileKey());
                  return CityJSONMapper.toRestCityJsonFileUrl(cityJson, fileUrl);
                })
            .toList();

    return new ThreeDResponseStatus()
        .id(cityJSONRequest.getId())
        .delimitations(restDelimitations)
        .status(CityJSONRequestStatusMapper.toGenericStatusRest(cityJSONRequest.getStatus()))
        .cityJsonFileUrls(restCityJsons);
  }

  public app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest createToDomain(
      String requestIdentifier,
      CreateCityJSONRequest createCityJSONRequest,
      String communityOwnerId) {
    List<Feature> delimitations =
        createCityJSONRequest.getDelimitations() == null
            ? List.of()
            : createCityJSONRequest.getDelimitations();
    var domainDelimitations = delimitations.stream().map(FeatureMapper::toDomainFeature).toList();

    return app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest.builder()
        .id(requestIdentifier)
        .creationDatetime(now())
        .communityOwnerId(communityOwnerId)
        .delimitations(domainDelimitations)
        .build();
  }

  public app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest createToDomain(
      String requestIdentifier, ThreeDRequest createCityJSONRequest, String communityOwnerId) {
    List<Feature> delimitations =
        createCityJSONRequest.getDelimitations() == null
            ? List.of()
            : createCityJSONRequest.getDelimitations();
    var domainDelimitations = delimitations.stream().map(FeatureMapper::toDomainFeature).toList();

    return app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest.builder()
        .id(requestIdentifier)
        .creationDatetime(now())
        .communityOwnerId(communityOwnerId)
        .delimitations(domainDelimitations)
        .build();
  }
}
