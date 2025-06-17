package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;
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
import java.util.function.BiConsumer;
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
      var featureWithDelimitationList =
          computeFeatureWithDelimitationFromDetection(detection, isSynchronous);
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

      var detectionWithDelimitations =
          detection.toBuilder()
              .pointDelimitation(pointDelimitation)
              .featureWithDelimitations(featureWithDelimitationList)
              .build();
      detectionRepository.save(detectionWithDelimitations);
    }
  }

  private List<FeatureWithDelimitation> computeFeatureWithDelimitationFromDetection(
      Detection detection, Boolean isSynchronous) {
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
              var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
              pointExtendedImageRequest.accept(providedFeature, layer, isSynchronous);
              var geometryType = providedFeature.getGeometry().getActualInstance();
              switch (geometryType) {
                case Point point -> {
                  var multiPolygonFromPointDomain =
                      geometryConverter.toFeature(
                          null,
                          zoom,
                          properties,
                          geometryConverter.retrieveNearestRoofMultiPolygon(point));
                  return new FeatureWithDelimitation(
                      toDomainFeature(providedFeature), List.of(multiPolygonFromPointDomain));
                }

                case Polygon providedPolygon -> {
                  var polygonList = providedPolygon.getCoordinates();
                  return retrieveFeatureWithDelimitation(providedFeature, zoom, polygonList);
                }

                case MultiPolygon providedMultiPolygon -> {
                  var polygonList = getPolygonFromProvidedMultiPolygon(providedMultiPolygon);
                  return retrieveFeatureWithDelimitation(providedFeature, zoom, polygonList);
                }
                default ->
                    throw new IllegalStateException("Unexpected geometry type: " + geometryType);
              }
            })
        .toList();
  }

  private List<List<List<BigDecimal>>> getPolygonFromProvidedMultiPolygon(
      MultiPolygon providedMultiPolygon) {
    var multiPolygonCoordinates = providedMultiPolygon.getCoordinates();
    if (multiPolygonCoordinates == null || multiPolygonCoordinates.size() != 1) {
      throw new UnsupportedOperationException(
          "Only one polygon allowed for retrieving delimitation retriever,"
              + " otherwise actual is null, empty or more than one polygon");
    }
    return multiPolygonCoordinates.getFirst();
  }

  private FeatureWithDelimitation retrieveFeatureWithDelimitation(
      Feature providedFeature, int zoom, List<List<List<BigDecimal>>> polygonList) {
    if (polygonList == null || polygonList.size() != 1) {
      throw new UnsupportedOperationException(
          "Only one polygon allowed for retrieving delimitation retriever,"
              + " otherwise actual is null, empty or more than one polygon");
    }
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
