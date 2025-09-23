package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.*;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetectionDelimitationRetriever implements Consumer<Detection> {
  private final GeometryConverter geometryConverter;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(Detection detection) {
    if (detection.hasToitureModelName()) {
      var featureWithDelimitationList =
          computeFeatureWithDelimitationFromDetectionFacade(detection);
      var featureWithDelimitationMap =
          featureWithDelimitationList.stream()
              .collect(groupingBy(FeatureWithDelimitation::feature))
              .entrySet()
              .stream()
              .collect(
                  toMap(
                      entry -> {
                        try {
                          app.bpartners.geojobs.repository.model.Feature providedFeature =
                              entry.getKey();
                          return new ObjectMapper().writeValueAsString(providedFeature);
                        } catch (JsonProcessingException e) {
                          throw new RuntimeException(e);
                        }
                      },
                      featureListEntry ->
                          featureListEntry.getValue().stream()
                              .map(FeatureWithDelimitation::delimitations)
                              .flatMap(List::stream)
                              .toList(),
                      (v1, v2) -> v1));

      var pointDelimitation = new HashMap<String, app.bpartners.geojobs.repository.model.Feature>();
      featureWithDelimitationMap.forEach(
          (key, value) -> {
            if (value.size() == 1) {
              pointDelimitation.put(key, value.getFirst());
            }
          });

      log.info(
          "debug featureWithDelimitationList {}",
          featureWithDelimitationList.stream()
              .map(FeatureWithDelimitation::toString)
              .collect(toList()));
      var detectionWithDelimitations =
          detection.toBuilder()
              .pointDelimitation(pointDelimitation)
              .featureWithDelimitations(featureWithDelimitationList)
              .build();
      detectionRepository.save(detectionWithDelimitations);
    }
  }

  private List<FeatureWithDelimitation> computeFeatureWithDelimitationFromDetectionFacade(
      Detection detection) {
    if (detection.getGeoJsonDelimitationType() == null) {
      log.warn("GeoJsonDelimitationTypeEnum is null, defaulting to ZONE");
      return computeFeatureWithDelimitationFromDetection(detection);
    }

    return switch (detection.getGeoJsonDelimitationType()) {
      case ZONE -> computeFeatureWithDelimitationFromDetection(detection);
      case ROOF -> computeFeatureWithDelimitationFromProvidedGeoJson(detection);
    };
  }

  private app.bpartners.geojobs.repository.model.Feature getRoofMultipolygonFromProvidedFeature(
      app.bpartners.geojobs.repository.model.Feature feature) {
    var restFeature = toRestFeature(feature);
    Map<String, Object> properties =
        restFeature.getProperties() == null ? new HashMap<>() : restFeature.getProperties();
    properties.put("zoom", feature.getZoom() == null ? HOUSES_0.getZoomLevel() : feature.getZoom());
    restFeature.setProperties(properties);

    var geometryType = getGeometryType(restFeature.getGeometry().getActualInstance());
    switch (geometryType) {
      case POLYGON:
        var multipolygonGeometry =
            new FeatureGeometry(
                new MultiPolygon()
                    .type(MultiPolygon.TypeEnum.MULTI_POLYGON)
                    .coordinates(List.of(restFeature.getGeometry().getPolygon().getCoordinates())));
        restFeature.setGeometry(multipolygonGeometry);
        return toDomainFeature(restFeature);
      case MULTI_POLYGON:
        return toDomainFeature(restFeature);
      default:
        throw new IllegalStateException("Point is not supported");
    }
  }

  private List<FeatureWithDelimitation> computeFeatureWithDelimitationFromProvidedGeoJson(
      Detection detection) {
    var providedGeoJsonZone = detection.getDomainProvidedGeoJsonZone();
    return providedGeoJsonZone.stream()
        .map(
            feature ->
                new FeatureWithDelimitation(
                    feature, List.of(getRoofMultipolygonFromProvidedFeature(feature))))
        .toList();
  }

  private List<FeatureWithDelimitation> computeFeatureWithDelimitationFromDetection(
      Detection detection) {
    return detection.getProvidedGeoJsonZone().stream()
        .map(
            providedFeature -> {
              var properties =
                  providedFeature.getProperties() == null
                      ? new HashMap<String, Object>()
                      : providedFeature.getProperties();
              var zoom =
                  properties.get("zoom") != null
                      ? (Integer) properties.get("zoom")
                      : HOUSES_0.getZoomLevel();
              var geometryType = providedFeature.getGeometry().getActualInstance();
              switch (geometryType) {
                case Point point -> {
                  var multiPolygonFromPointDomain =
                      geometryConverter.toFeature(
                          null,
                          zoom,
                          properties,
                          geometryConverter.retrieveNearestRoofMultiPolygon(point));
                  return List.of(
                      new FeatureWithDelimitation(
                          toDomainFeature(providedFeature), List.of(multiPolygonFromPointDomain)));
                }

                case Polygon providedPolygon -> {
                  var polygonList = providedPolygon.getCoordinates();
                  return List.of(
                      retrieveFeatureWithDelimitation(providedFeature, zoom, polygonList));
                }

                case MultiPolygon providedMultiPolygon -> {
                  return providedMultiPolygon.getCoordinates().stream()
                      .map(
                          polygonList ->
                              retrieveFeatureWithDelimitation(providedFeature, zoom, polygonList))
                      .toList();
                }
                default ->
                    throw new IllegalStateException("Unexpected geometry type: " + geometryType);
              }
            })
        .flatMap(List::stream)
        .toList();
  }

  private FeatureWithDelimitation retrieveFeatureWithDelimitation(
      Feature providedFeature, int zoom, List<List<List<BigDecimal>>> polygonList) {
    var polygonCoordinates = polygonList.getFirst();
    var roofMultiPolygonsInsideProvidedPolygon =
        geometryConverter.retrieveRoofPolygonsFrom(polygonCoordinates).stream()
            .map(
                multiPolygon ->
                    geometryConverter.toFeature(
                        randomUUID().toString(), zoom, new HashMap<>(), multiPolygon))
            .toList();
    return new FeatureWithDelimitation(
        toDomainFeature(providedFeature), roofMultiPolygonsInsideProvidedPolygon);
  }
}
