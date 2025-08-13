package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.lidar.LidarPolygonMetricProcessor;
import app.bpartners.geojobs.service.lidar.model.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionRoofSlopeAndHeightRequestedService
    implements Consumer<DetectionRoofSlopeAndHeightRequested> {
  private final DetectionRepository detectionRepository;
  private final LidarPolygonMetricProcessor lidarPolygonMetricProcessor;
  private final FeatureMapper featureMapper;

  public static final String ROOF_SLOPE_PROPERTY_NAME = "roof_slope_in_degrees";
  public static final String ROOF_HEIGHT_PROPERTY_NAME = "roof_height_in_meters";

  @Override
  public void accept(DetectionRoofSlopeAndHeightRequested requested) {
    var detection =
        detectionRepository
            .findById(requested.getDetectionId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Detection={" + requested.getDetectionId() + "} not found"));

    var featureWithDelimitations = detection.getFeatureWithDelimitations();
    if (featureWithDelimitations == null) {
      throw new IllegalArgumentException(
          "FeatureWithDelimitation is null for detection={" + requested.getDetectionId() + "}");
    }

    var roofGeometries = toPolygons(featureWithDelimitations);
    var dimensions = lidarPolygonMetricProcessor.apply(roofGeometries);
    var newFeaturesWithDelimitations =
        addSlopeAndHeightCalculationInFeatures(featureWithDelimitations, dimensions);

    detection.setFeatureWithDelimitations(newFeaturesWithDelimitations);
    detectionRepository.save(detection);
  }

  private List<Polygon> toPolygons(List<FeatureWithDelimitation> featureWithDelimitations) {
    List<Feature> flattedFeatures =
        featureWithDelimitations.stream()
            .map(FeatureWithDelimitation::delimitations)
            .flatMap(List::stream)
            .toList();
    return flattedFeatures.stream().map(featureMapper::domainToJtsPolygon).toList();
  }

  private List<FeatureWithDelimitation> addSlopeAndHeightCalculationInFeatures(
      List<FeatureWithDelimitation> featureWithDelimitations, List<Dimension> dimensions) {
    int dimIndex = 0;

    for (var featureWithDelimitation : featureWithDelimitations) {
      for (int j = 0; j < featureWithDelimitation.delimitations().size(); j++) {
        var delimitation = featureWithDelimitation.delimitations().get(j);
        var dimension = dimensions.get(dimIndex++);

        if (delimitation.getProperties() == null) {
          delimitation.setProperties(new HashMap<>());
        }

        delimitation.getProperties().put(ROOF_SLOPE_PROPERTY_NAME, dimension.getSlopeInDegrees());
        delimitation.getProperties().put(ROOF_HEIGHT_PROPERTY_NAME, dimension.getHeightInMeters());
      }
    }

    return featureWithDelimitations;
  }
}
