package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.model.DelimitationObjectType.BUILDING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.model.DelimitationObjectType;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class FeatureAddressConverter
    implements BiFunction<String, DelimitationObjectType, Feature> {
  private final GeoCodeApi geoCodeApi;
  private final FeaturePointConverter featurePointConverter;

  public FeatureAddressConverter(
      GeoCodeApi geoCodeApi, FeaturePointConverter featurePointConverter) {
    this.geoCodeApi = geoCodeApi;
    this.featurePointConverter = featurePointConverter;
  }

  @Override
  public Feature apply(String address, DelimitationObjectType delimitationObjectType) {
    if (BUILDING.equals(delimitationObjectType)) {
      Double longitude = null, latitude = null;
      try {
        var geoPosition = geoCodeApi.searchGeoPositionFromAddress(address);
        longitude = geoPosition.longitude();
        latitude = geoPosition.latitude();
      } catch (IOException | InterruptedException | com.google.maps.errors.ApiException e) {
        throwsExceptionOnAddress(address);
      }
      if (longitude == null) {
        throwsExceptionOnAddress(address);
      }
      return featurePointConverter.apply(
          new Point()
              .coordinates(List.of(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude))),
          delimitationObjectType);
    }
    throw new NotImplementedException(
        "Unable to convert address to Feature for delimitationObjectType "
            + delimitationObjectType);
  }

  private void throwsExceptionOnAddress(String address) {
    throw new ApiException(
        SERVER_EXCEPTION,
        "Unable to convert address to GPS coordinate (longitude, latitude) : " + address);
  }
}
