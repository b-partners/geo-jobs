package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.google.maps.GeoCodeApi;
import com.google.maps.errors.ApiException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoCodeService {
  private final GeoCodeApi geoCodeApi;
  private final GeometryConverter geometryConverter;

  public Feature geocode(String address) {
    if (address == null || address.isBlank()) {
      throw new BadRequestException("Address is mandatory");
    }
    try {
      var geoPosition = geoCodeApi.searchGeoPositionFromAddress(address);
      var longitude = BigDecimal.valueOf(geoPosition.longitude());
      var latitude = BigDecimal.valueOf(geoPosition.latitude());
      var nearestRoofMultiPolygon =
          geometryConverter.retrieveNearestRoofMultiPolygon(List.of(longitude, latitude));

      var properties = new HashMap<String, Object>();
      properties.put("address", address);

      return geometryConverter.toFeature(
          null, HOUSES_0.getZoomLevel(), properties, nearestRoofMultiPolygon);
    } catch (IOException | InterruptedException | ApiException e) {
      throw new BadRequestException("Unable to geocode address : " + address);
    }
  }
}
