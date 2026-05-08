package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.roofer3dbag.Roofer3DBagApiClient;
import app.bpartners.geojobs.service.roofer3dbag.model.CityJsonGenerationRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CityJSON3DBagRooferProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private static final String JSONL_EXTENSION = ".jsonl";
  private final BucketComponent bucketComponent;
  private final FeatureMapper featureMapper;
  private final LidarApiFacade lidarApiFacade;
  private final Roofer3DBagApiClient roofer3DBagApiClient;
  private final FileWriter fileWriter;

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    var delimitationFeatureGeoJsonFileURL = retrieveGeometriesWithPresignedURL(request);
    var cityJsonGenerationResponses =
        delimitationFeatureGeoJsonFileURL.entrySet().stream()
            .map(
                entry -> {
                  var geoJsonBuildingPresignedUrl = entry.getKey();

                  return roofer3DBagApiClient.generateCityJson(
                      CityJsonGenerationRequest.builder()
                          .geoJsonBuildingPresignedUrl(geoJsonBuildingPresignedUrl)
                          .lidarPresignedUrls(entry.getValue().stream().toList())
                          .build());
                })
            .toList();

    return cityJsonGenerationResponses.stream()
        .map(
            cityJsonGenerationResponse -> {
              String bucketFileKey = randomUUID() + JSONL_EXTENSION;
              File rooferCityJsonFile;
              try {
                URL rooferCityJsonURL;
                rooferCityJsonURL = new URI(cityJsonGenerationResponse.getCityJsonUrl()).toURL();
                rooferCityJsonFile = File.createTempFile(bucketFileKey, JSONL_EXTENSION);
                try (InputStream in = rooferCityJsonURL.openStream()) {
                  Files.copy(in, rooferCityJsonFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
              }

              bucketComponent.upload(rooferCityJsonFile, bucketFileKey);

              var cityJsonId = randomUUID().toString();
              return CityJSON.builder()
                  .id(cityJsonId)
                  .request(request)
                  .s3FileKey(bucketFileKey)
                  .build();
            })
        .toList();
  }

  private Map<String, Set<String>> retrieveGeometriesWithPresignedURL(CityJSONRequest request) {
    return request.getRequestDelimitations().stream()
        .map(
            feature -> {
              try {
                var presignURL = getGeoJsonBuildingPresignedURL(feature);
                var uniqueLidarFilesUrls = getUniqueLidarFilesUrls(feature);
                return Map.of(presignURL.toString(), uniqueLidarFilesUrls);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            })
        .flatMap(map -> map.entrySet().stream())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Set<String> getUniqueLidarFilesUrls(Feature feature) {
    var geometry = featureMapper.domainToGeometryWithMultipolygonHandler(feature);
    var geometries = Collections.singleton(geometry);
    return lidarApiFacade.getUniqueLidarFilesUrls(geometries).keySet();
  }

  private URL getGeoJsonBuildingPresignedURL(Feature feature) throws IOException {
    var tmpGeoJsonBucketKey = randomUUID().toString();
    var multiPolygon = getMultiPolygon(feature);

    var geoJson =
        new GeoJson(List.of(new GeoJson.GeoFeature(feature.getProperties(), multiPolygon)));

    var tmpGeoJsonFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(UTF_8),
            createTempDirectory(),
            randomUUID() + ".geojson");

    bucketComponent.upload(tmpGeoJsonFile, tmpGeoJsonBucketKey);

    return bucketComponent.presign(tmpGeoJsonBucketKey, Duration.ofHours(1L));
  }

  private MultiPolygon getMultiPolygon(Feature feature) {
    var restFeature = toRestFeature(feature);
    var actualInstance = restFeature.getGeometry().getActualInstance();
    if (actualInstance instanceof MultiPolygon m) {
      return m;
    } else if (actualInstance instanceof Polygon p) {
      return new MultiPolygon().type(MULTI_POLYGON).coordinates(List.of(p.getCoordinates()));
    }
    throw new NotImplementedException(
        "Unsupported geometry type for validation: " + actualInstance);
  }
}
