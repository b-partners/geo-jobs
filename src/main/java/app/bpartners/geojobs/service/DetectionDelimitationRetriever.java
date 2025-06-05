package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionDelimitationRetriever implements BiConsumer<Detection, Boolean> {
  private final PointExtendedImageRequest pointExtendedImageRequest;
  private final GeometryConverter geometryConverter;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(Detection detection, Boolean isSynchronous) {
    if (detection.hasToitureModelName()) {
      var collectedPointWithItsMultiPolygon =
          detection.getProvidedGeoJsonZone().stream()
              .map(
                  feature -> {
                    var layer =
                        detection.getGeoServerProperties().getGeoServerParameter().getLayers();
                    var restPointFeature =
                        pointExtendedImageRequest.apply(feature, layer, isSynchronous);
                    var restPoint = restPointFeature.getGeometry().getPoint();
                    if (restPoint == null) {
                      return null;
                    }
                    var properties =
                        feature.getProperties() == null
                            ? new HashMap<String, Object>()
                            : feature.getProperties();
                    var zoom =
                        properties.get("zoom") != null
                            ? (Integer) properties.get("zoom")
                            : HOUSES_0.getZoomLevel();
                    var pointDomain = geometryConverter.toFeature(zoom, new HashMap<>(), restPoint);
                    var geometryType = feature.getGeometry().getActualInstance();
                    switch (geometryType) {
                      case Point point -> {
                        var multiPolygonFromPointDomain =
                            geometryConverter.toFeature(
                                null,
                                zoom,
                                properties,
                                geometryConverter.retrieveNearestRoofMultiPolygon(point));
                        return new HashMap<>(Map.of(pointDomain, multiPolygonFromPointDomain));
                      }

                      case Polygon ignored -> {
                        return getPolygonFeatureDelimitationMap(feature, properties, pointDomain);
                      }

                      case MultiPolygon ignored -> {
                        return getPolygonFeatureDelimitationMap(feature, properties, pointDomain);
                      }
                      default ->
                          throw new IllegalStateException(
                              "Unexpected geometry type: " + geometryType);
                    }
                  })
              .filter(Objects::nonNull)
              .flatMap(map -> map.entrySet().stream())
              .collect(
                  Collectors.toMap(
                      entry -> {
                        try {
                          return new ObjectMapper()
                              .findAndRegisterModules()
                              .writeValueAsString(entry.getKey());
                        } catch (JsonProcessingException e) {
                          throw new RuntimeException(e);
                        }
                      },
                      Map.Entry::getValue,
                      (v1, v2) -> v1));
      var detectionBuilder = detection.toBuilder();
      if (detection.getProvidedGeoJsonZone().stream()
          .anyMatch(
              feature ->
                  feature.getProperties() != null
                      && feature.getProperties().containsKey("centroid"))) {
        detectionBuilder.providedGeoJsonZone(
            detection.getProvidedGeoJsonZone().stream()
                .map(FeatureMapper::toDomainFeature)
                .toList());
      }
      var detectionWithMultiPolygonFromPoint =
          detectionBuilder
              .pointDelimitation(new HashMap<>(collectedPointWithItsMultiPolygon))
              .build();
      detectionRepository.save(detectionWithMultiPolygonFromPoint);
    }
  }

  private HashMap<
          app.bpartners.geojobs.repository.model.Feature,
          app.bpartners.geojobs.repository.model.Feature>
      getPolygonFeatureDelimitationMap(
          Feature feature,
          Map<String, Object> properties,
          app.bpartners.geojobs.repository.model.Feature pointDomain) {
    var delimitationFeature = toDomainFeature(feature, new HashMap<>());
    try {
      properties.put(
          "centroid", new ObjectMapper().findAndRegisterModules().writeValueAsString(pointDomain));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return new HashMap<>(Map.of(pointDomain, delimitationFeature));
  }
}
