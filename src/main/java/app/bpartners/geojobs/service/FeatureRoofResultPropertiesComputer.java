package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.LIDAR_DATA_STATUS_PROPERTY_NAME;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade;
import app.bpartners.geojobs.service.event.DetectionRoofPropertiesRequestedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureRoofResultPropertiesComputer {
  private final GeometrySquareMeterArea geometrySquareMeterArea;
  private final ObjectMapper objectMapper;

  public HashMap<String, Object> apply(
      Feature feature,
      Geometry geometryUsedForAreaComputing,
      Geometry roofGeometryUsedForRateComputing,
      Collection<PolygonObjectType> detectedObjectPolygonGeometriesUsedForRateComputing) {

    var featureProperties =
        feature.getProperties() == null ? new HashMap<String, Object>() : feature.getProperties();
    var properties = new HashMap<String, Object>();

    properties.put("roof_area_in_m2", geometrySquareMeterArea.apply(geometryUsedForAreaComputing));

    var addresses = retrieveAddressesProperty(feature);
    properties.put("addresses", addresses);

    var rateComputer =
        new AreaRateComputerFacade(
            roofGeometryUsedForRateComputing, detectedObjectPolygonGeometriesUsedForRateComputing);
    var usureRate = rateComputer.getUsureAreaRate();
    var humiditeRate = rateComputer.getHumidityAreaRate();
    var moisissureRate = rateComputer.getMoisissureAreaRate();
    var globalRateValue = rateComputer.getGlobalRate();
    var globalRateType = rateComputer.getRate();

    properties.put("usure_rate", usureRate);
    properties.put("humidite_rate", humiditeRate);
    properties.put("moisissure_rate", moisissureRate);
    properties.put("global_rate_value", globalRateValue);
    properties.put("global_rate_type", globalRateType);

    // Lidar properties
    if (featureProperties.containsKey(LIDAR_DATA_STATUS_PROPERTY_NAME)) {
      properties.put("roof_slope_in_degrees", featureProperties.get(ROOF_SLOPE_PROPERTY_NAME));
      properties.put("roof_height_in_meters", featureProperties.get(ROOF_HEIGHT_PROPERTY_NAME));
      properties.put(
          "roof_slope_data_status", featureProperties.get(LIDAR_DATA_STATUS_PROPERTY_NAME));
      properties.put(
          "roof_height_data_status", featureProperties.get(LIDAR_DATA_STATUS_PROPERTY_NAME));
    }

    var detectedRoofCovering = retrieveCoveringProperties(feature);
    if (detectedRoofCovering != null) {
      properties.put(
          "revetement_1",
          detectedRoofCovering.primary() == null ? null : detectedRoofCovering.primary().name());
      properties.put(
          "revetement_2",
          detectedRoofCovering.secondary() == null
              ? null
              : detectedRoofCovering.secondary().name());
    }

    return properties;
  }

  private DetectionRoofPropertiesRequestedService.DetectedRoofCovering retrieveCoveringProperties(
      Feature feature) {
    if (feature.getProperties() == null
        || (feature.getProperties().isEmpty() || feature.getProperties().get("covering") == null)) {
      return null;
    }
    try {
      return objectMapper.readValue(
          feature.getProperties().get("covering").toString(),
          DetectionRoofPropertiesRequestedService.DetectedRoofCovering.class);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private List<String> retrieveAddressesProperty(Feature feature) {
    if (feature.getProperties() == null
        || (feature.getProperties().isEmpty()
            || feature.getProperties().get("addresses") == null)) {
      return null;
    }
    try {
      return objectMapper.readValue(
          feature.getProperties().get("addresses").toString(), new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
