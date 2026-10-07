package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toDomainFeature;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.UNAVAILABLE;
import static app.bpartners.geojobs.service.threed.model.DelimitationType.ENTIRE_ROOF_DELIMITATION;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.CityJSONThreedProcessor;
import app.bpartners.geojobs.service.threed.model.CityJsonRoofMetrics;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRoofSlopeAndHeightRequestedService
    implements Consumer<FeatureRoofSlopeAndHeightRequested> {
  private static final String ROOF_SLOPE_DATA_STATUS_PROPERTY_NAME = "roof_slope_data_status";
  private static final String ROOF_HEIGHT_DATA_STATUS_PROPERTY_NAME = "roof_height_data_status";
  private final DetectionRepository detectionRepository;
  private final CityJSONThreedProcessor cityJSONThreedProcessor;
  private final EventProducer eventProducer;
  private final EntityManager entityManager;

  @Override
  public void accept(FeatureRoofSlopeAndHeightRequested featureRoofSlopeAndHeightRequested) {
    var detectionIdentifier = featureRoofSlopeAndHeightRequested.getDetectionIdentifier();
    var currentFeature = featureRoofSlopeAndHeightRequested.getFeature();
    entityManager.clear();

    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();

    var delimitationFeatures = detection.getDelimitationOf(currentFeature);

    if (isAlreadyProcessedAsSuccess(delimitationFeatures)) {
      log.warn(
          "Detection(id={}) for feature={} lidar properties has already been processed",
          detectionIdentifier,
          currentFeature);
      return;
    }

    var delimitationFeaturesWithHeightAndSlopeProperties =
        delimitationFeatures.stream().map(this::withHeightAndSlopeProperties).toList();

    var domainDelimitationFeaturesWithHeightAndSlopeProperties =
        delimitationFeaturesWithHeightAndSlopeProperties.stream()
            .map(FeatureMapper::toDomainFeature)
            .toList();

    detection.addFeatureDelimitations(
        toDomainFeature(currentFeature), domainDelimitationFeaturesWithHeightAndSlopeProperties);

    detectionRepository.save(detection);

    eventProducer.accept(List.of(new FeatureVggRequested(detectionIdentifier, currentFeature)));
  }

  private boolean isAlreadyProcessedAsSuccess(List<Feature> delimitationFeatures) {
    return delimitationFeatures.stream()
        .anyMatch(
            feature ->
                feature.getProperties() != null
                    && AVAILABLE.equals(
                        feature.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME)));
  }

  private Feature withHeightAndSlopeProperties(Feature delimitation) {
    var metrics = computeMetrics(delimitation);
    var lidarDataStatus = metrics.hasRoof() ? AVAILABLE : UNAVAILABLE;

    var actualProperties = new HashMap<String, Object>();
    if (delimitation.getProperties() != null) {
      actualProperties.putAll(delimitation.getProperties());
    }
    actualProperties.put(ROOF_SLOPE_PROPERTY_NAME, metrics.slopeInDegrees());
    actualProperties.put(ROOF_HEIGHT_PROPERTY_NAME, metrics.heightInMeters());
    actualProperties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);
    actualProperties.put(ROOF_SLOPE_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);
    actualProperties.put(ROOF_HEIGHT_DATA_STATUS_PROPERTY_NAME, lidarDataStatus);

    return new Feature()
        .type(delimitation.getType())
        .geometry(delimitation.getGeometry())
        .properties(actualProperties);
  }

  /** Generates the roof LOD2 model with threed and reads its slope and height. */
  private CityJsonRoofMetrics computeMetrics(Feature delimitation) {
    var cityJson =
        cityJSONThreedProcessor.generate(
            randomUUID().toString(), toDomainFeature(delimitation), ENTIRE_ROOF_DELIMITATION, null);
    try {
      return CityJsonRoofMetrics.from(cityJson);
    } finally {
      try {
        Files.deleteIfExists(cityJson.toPath());
      } catch (IOException e) {
        log.warn("Cannot delete {}", cityJson, e);
      }
    }
  }
}
