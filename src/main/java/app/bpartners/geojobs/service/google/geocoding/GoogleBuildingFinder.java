package app.bpartners.geojobs.service.google.geocoding;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.geometry.RoofDetails;
import app.bpartners.geojobs.service.BuildingFinder;
import app.bpartners.geojobs.service.google.geocoding.api.GeoJsonFeature;
import app.bpartners.geojobs.service.google.geocoding.api.GoogleGeocodingClientApi;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleBuildingFinder implements BuildingFinder {
  private final GoogleGeocodingClientApi geocodingClientApi;
  private final ObjectMapper objectMapper;

  @Override
  public MultiPolygon getBuildingMultiPolygon(Point point) {
    return getBuildingMultiPolygon(point.getCoordinates());
  }

  @Override
  public MultiPolygon getBuildingMultiPolygon(List<BigDecimal> coordinates) {
    var longitude = coordinates.get(0);
    var latitude = coordinates.get(1);
    return getMultiPolygonFromCoordinates(latitude, longitude);
  }

  @Override
  public MultiPolygon getBuildingMultiPolygon(String address) {
    return getMultiPolygonFromAddress(address);
  }

  @Override
  public List<RoofDetails> retrieveRoofPolygonsFrom(
      List<List<BigDecimal>> lonLatPolygonCoordinates) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @SneakyThrows
  private MultiPolygon getMultiPolygonFromCoordinates(BigDecimal latitude, BigDecimal longitude) {
    var optionalGeoJsonFeature =
        geocodingClientApi.findBuildingByLocation(latitude.doubleValue(), longitude.doubleValue());
    var exceptionMessage = "coordinates (lat=" + latitude + ", lon=" + longitude + ")";

    return getMultiPolygonFromGoogleGeocoding(optionalGeoJsonFeature, exceptionMessage);
  }

  @SneakyThrows
  private MultiPolygon getMultiPolygonFromAddress(String address) {
    var optionalGeoJsonFeature = geocodingClientApi.findBuildingByAddress(address);
    var exceptionMessage = "address=" + address;

    return getMultiPolygonFromGoogleGeocoding(optionalGeoJsonFeature, exceptionMessage);
  }

  private MultiPolygon getMultiPolygonFromGoogleGeocoding(
      Optional<GeoJsonFeature> optionalGeoJsonFeature, String exceptionMessage)
      throws JsonProcessingException {
    if (optionalGeoJsonFeature.isEmpty()) {
      log.info(
          "Unable to retrieve geojson feature from google geocoding for {} ", exceptionMessage);
      return null;
    }
    var geometry =
        objectMapper.readValue(optionalGeoJsonFeature.get().geometry().toString(), Geometry.class);
    if (geometry instanceof MultiPolygon multiPolygon) {
      return multiPolygon;
    } else if (geometry instanceof Polygon polygon) {
      return geometryFactory.createMultiPolygon(new Polygon[] {polygon});
    }
    log.info(
        "Unable to retrieve building from google geocoding for {} with" + " unexpected geometry {}",
        exceptionMessage,
        geometry.getClass().getSimpleName());
    return null;
  }
}
