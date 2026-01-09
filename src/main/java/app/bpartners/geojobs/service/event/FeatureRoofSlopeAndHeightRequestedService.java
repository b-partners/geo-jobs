package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.lidar.planes.Plane3D;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRoofSlopeAndHeightRequestedService
    implements Consumer<FeatureRoofSlopeAndHeightRequested> {
  private final DetectionRepository detectionRepository;
  private final LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessor;
  private final FeatureMapper featureMapper;
  private final EntityManager entityManager;
  private final FeatureVggRequestedService zoneVggRequestedService;

  @Override
  public void accept(FeatureRoofSlopeAndHeightRequested featureRoofSlopeAndHeightRequested) {
    var detectionIdentifier = featureRoofSlopeAndHeightRequested.getDetectionIdentifier();
    var feature = featureRoofSlopeAndHeightRequested.getFeature();
    int featureNb = featureRoofSlopeAndHeightRequested.getFeatureNb();

    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var featureWithDelimitations =
        detection.getFeatureWithDelimitations().stream()
            .filter(
                featureWithDelimitation ->
                    Objects.equals(featureWithDelimitation.getRestFeature(), feature))
            .toList();

    if (isAlreadyProcessedAsSuccess(featureWithDelimitations)) {
      log.warn(
          "Detection(id={}) for feature={} lidar properties has already been processed",
          detectionIdentifier,
          feature);
      return;
    }

    var roofGeometries = toGeometries(featureWithDelimitations);
    var roofsAnalysesResult = lidarRoofsAnalysisProcessor.from(roofGeometries);
    var featuresWithDelimitationsWithRoofProperties =
        addRoofProperties(featureWithDelimitations, roofsAnalysesResult);

    // Clear cache as between process begin and end, detection may be updated
    entityManager.clear();
    var actualDetection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    // This may produces concurrency exception
    detectionRepository.save(
        actualDetection.toBuilder()
            .featureWithDelimitations(featuresWithDelimitationsWithRoofProperties)
            .build());

    zoneVggRequestedService.accept(
        new FeatureVggRequested(detectionIdentifier, feature, featureNb));
  }

  private boolean isAlreadyProcessedAsSuccess(
      List<FeatureWithDelimitation> featureWithDelimitations) {
    return featureWithDelimitations.stream()
        .map(FeatureWithDelimitation::delimitations)
        .flatMap(List::stream)
        .anyMatch(
            feature ->
                feature.getProperties() != null
                    && AVAILABLE.equals(
                        feature.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME)));
  }

  private Set<Geometry> toGeometries(List<FeatureWithDelimitation> featureWithDelimitations) {
    Set<app.bpartners.geojobs.repository.model.Feature> flattedFeatures =
        featureWithDelimitations.stream()
            .map(FeatureWithDelimitation::delimitations)
            .flatMap(List::stream)
            .collect(toSet());
    return flattedFeatures.stream().map(featureMapper::domainToGeometry).collect(toSet());
  }

  private List<FeatureWithDelimitation> addRoofProperties(
      List<FeatureWithDelimitation> featureWithDelimitations,
      LidarRoofsAnalysisProcessor.RoofsAnalysisResult roofsAnalysisResult) {
    for (var featureWithDelimitation : featureWithDelimitations) {
      for (var delimitation : featureWithDelimitation.delimitations()) {
        if (delimitation.getProperties() == null) {
          delimitation.setProperties(new HashMap<>());
        }

        var properties = delimitation.getProperties();
        var roofProperties =
            roofsAnalysisResult.getProperties(featureMapper.domainToGeometry(delimitation));

        var planes = roofProperties.getRoofPlanes();
        var firstPlane = planes.isEmpty() ? Plane3D.empty() : planes.getFirst();
        properties.put(ROOF_SLOPE_PROPERTY_NAME, firstPlane.getSlopeInDegrees().getValue());
        properties.put(ROOF_HEIGHT_PROPERTY_NAME, roofProperties.getHeightInMeters().getValue());
        properties.put(LIDAR_DATA_STATUS_PROPERTY_NAME, roofProperties.getData().status());
      }
    }

    return featureWithDelimitations;
  }
}
