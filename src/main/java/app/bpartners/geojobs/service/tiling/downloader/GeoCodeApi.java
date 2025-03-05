package app.bpartners.geojobs.service.tiling.downloader;

import app.bpartners.geojobs.endpoint.rest.model.GeoPosition;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeoCodeApi implements Function<String, GeoPosition> {
  private final GeoApiContext geoApiContext;
  private final Double DEFAULT_GEOCODE_SCORE = 0.0;

  public GeoCodeApi(@Value("${google.geocode.api.key}") String apiKey) {
    this.geoApiContext = new GeoApiContext.Builder().apiKey(apiKey).build();
  }

  @SneakyThrows
  @Override
  public GeoPosition apply(String address) {
    GeocodingResult[] geocodingResults = GeocodingApi.geocode(this.geoApiContext, address).await();
    GeocodingResult response = geocodingResults[0];
    LatLng location = response.geometry.location;
    return new GeoPosition()
        .score(DEFAULT_GEOCODE_SCORE)
        .latitude(location.lat)
        .longitude(location.lng);
  }
}
