package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.threed.model.DelimitationType.PANEL_BY_PANEL_DELIMITATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.CommunityAuthorizationRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.cityjson.CityJSONRequest;
import app.bpartners.geojobs.service.cityjson.texture.CityJsonTextureComputer;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.threed.ThreedApiClient;
import app.bpartners.geojobs.service.threed.model.CreateLrgFromFeatureFileUrl;
import app.bpartners.geojobs.service.threed.model.DelimitationType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityJSONThreedProcessor implements Function<CityJSONRequest, List<CityJSON>> {
  private static final String JSON_EXTENSION = ".json";
  private static final String GEOJSON_EXTENSION = ".geojson";

  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;
  private final CityJsonTextureComputer textureComputer;
  private final ThreedApiClient threedApiClient;
  private final CityJSONDownloader cityJSONDownloader;
  private final CommunityAuthorizationRepository communityRepository;

  @Component
  public static class CityJSONDownloader {
    public File download(String url) {
      try {
        var cityjsonFileUrl = new URI(url).toURL();
        var file = File.createTempFile(randomUUID().toString(), JSON_EXTENSION);

        try (var in = cityjsonFileUrl.openStream()) {
          Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return file;

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String getApiKey(String communityId) {
    var community = communityRepository.findById(communityId);
    if (community.isEmpty())
      throw new IllegalArgumentException("No community was found for id = " + communityId);
    return community.get().getMostRecentApiKey().getKeyValue();
  }

  @Override
  public List<CityJSON> apply(CityJSONRequest request) {
    var apiKey = getApiKey(request.getCommunityOwnerId());
    var id = request.getId();
    var delimitationType = getDelimitationType(request);

    return request.getRequestDelimitations().stream()
        .map(
            feature -> {
              var cityjsonFile = generate(id, feature, delimitationType, apiKey);
              var textured = textureComputer.applyTexture(request, cityjsonFile);

              var bucketFileKey = randomUUID() + JSON_EXTENSION;
              bucketComponent.upload(textured, bucketFileKey);
              delete(cityjsonFile, textured);

              return CityJSON.builder()
                  .id(randomUUID().toString())
                  .request(request)
                  .s3FileKey(bucketFileKey)
                  .build();
            })
        .toList();
  }

  /** Generates the CityJSON of the feature with threed and downloads it. */
  public File generate(
      String id, Feature feature, DelimitationType delimitationType, String apiKey) {
    var featureFileUrl = getGeoJsonBuildingPresignedURL(feature).toString();
    log.info("Presigned URL for building: {}", featureFileUrl);
    var lrg =
        threedApiClient.generate(
            id,
            CreateLrgFromFeatureFileUrl.builder()
                .featureFileUrl(featureFileUrl)
                .delimitationType(delimitationType)
                .build(),
            apiKey);
    return cityJSONDownloader.download(lrg.getFileUrl());
  }

  private static void delete(File... files) {
    for (var file : files) {
      try {
        Files.deleteIfExists(file.toPath());
      } catch (IOException e) {
        log.warn("Cannot delete {}", file, e);
      }
    }
  }

  @SneakyThrows
  private URL getGeoJsonBuildingPresignedURL(Feature feature) {
    var tmpGeoJsonBucketKey = randomUUID() + GEOJSON_EXTENSION;
    var multiPolygon = getMultiPolygon(feature);
    var geoJson = new GeoJson(new GeoJson.GeoFeature(feature.getProperties(), multiPolygon));

    var tmpGeoJsonFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(UTF_8), createTempDirectory(), tmpGeoJsonBucketKey);

    bucketComponent.upload(tmpGeoJsonFile, tmpGeoJsonBucketKey);

    return bucketComponent.presign(tmpGeoJsonBucketKey, Duration.ofHours(1L));
  }

  private MultiPolygon getMultiPolygon(Feature feature) {
    var restFeature = toRestFeature(feature);
    var actualInstance = requireNonNull(restFeature.getGeometry()).getActualInstance();
    if (actualInstance instanceof MultiPolygon m) {
      return m;
    } else if (actualInstance instanceof Polygon p) {
      return new MultiPolygon()
          .type(MULTI_POLYGON)
          .coordinates(List.of(requireNonNull(p.getCoordinates())));
    }
    throw new NotImplementedException(
        "Unsupported geometry type for validation: " + actualInstance);
  }

  private DelimitationType getDelimitationType(CityJSONRequest request) {
    var delimitationObjectType = request.getDelimitationObjectType();
    return switch (delimitationObjectType) {
      case BUILDING_ROOF_SEGMENT_FACE -> PANEL_BY_PANEL_DELIMITATION;
      case null, default -> DelimitationType.ENTIRE_ROOF_DELIMITATION;
    };
  }
}
