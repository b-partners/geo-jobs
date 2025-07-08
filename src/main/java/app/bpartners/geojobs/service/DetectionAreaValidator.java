package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionAreaValidator implements Consumer<Detection> {
  private static final String INDRE_ET_LOIRE_2024_5_CM = "INDRE_ET_LOIRE_2024_5CM";
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final GeometryConverter geometryConverter;

  @Override
  public void accept(Detection detection) {
    var geoJsonZone = detection.getGeoJsonZone();
    var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
    var unifiedProvidedPolygon =
        geoJsonZone.stream()
            .map(
                feature -> {
                  var geometryType = feature.getGeometry().getActualInstance();
                  org.locationtech.jts.geom.MultiPolygon geometry;
                  switch (geometryType) {
                    case Point ignored -> geometry = null;
                    case Polygon polygon ->
                        geometry = geometryConverter.apply(List.of(polygon.getCoordinates()));
                    case MultiPolygon multiPolygon ->
                        geometry = geometryConverter.apply(multiPolygon.getCoordinates());
                    default ->
                        throw new UnsupportedOperationException(
                            "Unsupported geometry type for validation: " + geometryType);
                  }
                  return geometry;
                })
            .filter(Objects::nonNull)
            .reduce(unifyMultiPolygon());
    if (unifiedProvidedPolygon.isEmpty()) {
      log.warn("No unified geometry found from provided geo-json: {}", geoJsonZone);
      return;
    }
    var actualArea = geometrySquareMeterArea.apply(unifiedProvidedPolygon.get());
    if (INDRE_ET_LOIRE_2024_5_CM.equals(layer) && actualArea > 12_000.0) {
      throw new NotImplementedException(
          "Provided multiPolygon must be under 12 000 meters for zone inside layers "
              + INDRE_ET_LOIRE_2024_5_CM
              + " for now, actual area: "
              + actualArea);
    }
  }
}
