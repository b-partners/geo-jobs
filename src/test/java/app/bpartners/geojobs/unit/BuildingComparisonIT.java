package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE;
import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.service.PolygonInsideCircleDistanceComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.geocoding.GoogleBuildingFinder;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import app.bpartners.geojobs.service.google.maps.AddressValidator;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import app.bpartners.geojobs.service.gouv.fr.rnb.RnbBuildingFinder;
import app.bpartners.geojobs.service.osm.NominatimClient;
import app.bpartners.geojobs.service.osm.OsmBuildingFinder;
import app.bpartners.geojobs.utils.BuildingComparisonCSVLogger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.n52.jackson.datatype.jts.JtsModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestTemplate;

@Disabled("TODO: local use only")
@Slf4j
public class BuildingComparisonIT {
  private static final String OUTPUT_PATH = "/tmp/output";
  private final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
  RestTemplate restTemplate = new RestTemplate();
  ObjectMapper objectMapper = getObjectMapper();

  private static @NotNull ObjectMapper getObjectMapper() {
    var mapper = new ObjectMapper();
    mapper.registerModule(new JtsModule());
    return mapper;
  }

  GeometryConverter geometryConverter = new GeometryConverter();
  RnbBuildingFinder rnbBuildingFinder =
      new RnbBuildingFinder(
          geometryConverter,
          new PolygonInsideCircleDistanceComputer(),
          new GeoCodeApi(GOOGLE_API_KEY, new AddressValidator()));
  OsmBuildingFinder osmBuildingFinder =
      new OsmBuildingFinder(new NominatimClient(objectMapper), geometryConverter, objectMapper);
  GoogleBuildingFinder googleBuildingFinder =
      new GoogleBuildingFinder(
          new GoogleGeocodingClientApi(restTemplate, GOOGLE_API_KEY), objectMapper);
  private final BuildingComparisonCSVLogger csvLogger =
      new BuildingComparisonCSVLogger(OUTPUT_PATH);

  @SneakyThrows
  @Test
  void compare_geocoding_result() {
    csvLogger.init();
    var featureWithExtractedAddresses = featureWithExtractedAddresses();

    featureWithExtractedAddresses.forEach(
        (address, referenceFeature) -> {
          try {
            var referenceMultiPolygon =
                referenceFeature.stream()
                    .map(GeometryConverter::retrieveMultiPolygonFromFeature)
                    .reduce(unifyMultiPolygon())
                    .orElseThrow(
                        () ->
                            new IllegalArgumentException(
                                "Unable to unify multiPolygon: " + address));

            var rnbMultiPolygon = rnbBuildingFinder.getBuildingMultiPolygon(address);
            var osmMultiPolygon = osmBuildingFinder.getBuildingMultiPolygon(address);
            var googleMultiPolygon = googleBuildingFinder.getBuildingMultiPolygon(address);
            var rnbResult =
                new SourceComparison(referenceMultiPolygon, rnbMultiPolygon, address, Source.RNB);
            var osmResult =
                new SourceComparison(referenceMultiPolygon, osmMultiPolygon, address, Source.OSM);
            var googleResult =
                new SourceComparison(
                    referenceMultiPolygon, googleMultiPolygon, address, Source.GOOGLE_MAPS);
            var buildingResultComparison =
                new BuildingResultComparison(rnbResult, osmResult, googleResult);

            assertNotNull(buildingResultComparison);

            log.info("Address : {}", address);

            log.info(
                "RNB inter : {}, diff : {}",
                buildingResultComparison.rnb.intersectionMatching(),
                buildingResultComparison.rnb.differenceMatching());

            log.info(
                "OSM inter : {}, diff : {}",
                buildingResultComparison.osm.intersectionMatching(),
                buildingResultComparison.osm.differenceMatching());

            log.info(
                "Google inter : {}, diff : {}",
                buildingResultComparison.google.intersectionMatching(),
                buildingResultComparison.google.differenceMatching());

            try {
              csvLogger.writeLine(address, buildingResultComparison);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          } catch (Exception e) {
            log.info("Unable to handle address {}, error: {}", address, e.getMessage());
          }
        });
  }

  private List<Feature> getExpectedFeatures() throws IOException {
    return objectMapper.readValue(
        new ClassPathResource("geojson/cv38_vizille_VDEF.geojson").getFile(),
        new TypeReference<>() {});
  }

  @SneakyThrows
  private Map<String, List<Feature>> featureWithExtractedAddresses() {
    return getExpectedFeatures().stream()
        .filter(
            feature ->
                feature.getProperties() != null && feature.getProperties().get("name") != null)
        .collect(
            Collectors.groupingBy(
                feature ->
                    feature.getProperties().get("name").toString() + " Vizille 38220 France"));
  }

  public record BuildingResultComparison(
      SourceComparison rnb, SourceComparison osm, SourceComparison google) {}

  public record SourceComparison(
      MultiPolygon reference, MultiPolygon source, String address, Source sourceType) {
    static GeometryConverter geometryConverter = new GeometryConverter();

    public Double intersectionMatching() {
      if (source == null) {
        return null;
      }
      if (!reference.intersects(source)) {
        return 0.0;
      }
      return (reference.intersection(source).getArea() / reference.getArea()) * 100.0;
    }

    public Double differenceMatching() {
      if (source == null) {
        return null;
      }
      if (source.difference(reference).isEmpty()) {
        return 0.0;
      }
      return (source.difference(reference).getArea() / reference.getArea()) * 100.0;
    }

    public Feature feature() {
      if (source == null) {
        return null;
      }
      var properties = new HashMap<String, Object>();
      properties.put("source", sourceType);
      properties.put("address", address);
      properties.put("intersectionMatching", intersectionMatching());
      properties.put("differenceMatching", differenceMatching());
      return new Feature()
          .type(FEATURE)
          .properties(properties)
          .geometry(
              new FeatureGeometry(
                  new app.bpartners.geojobs.endpoint.rest.model.MultiPolygon()
                      .type(MULTI_POLYGON)
                      .coordinates(geometryConverter.multiPolygonToNestedList(source))));
    }
  }

  private enum Source {
    GOOGLE_MAPS,
    OSM,
    RNB
  }
}
