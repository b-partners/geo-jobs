package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.lidar.LidarPolygonMetricProcessor;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionRoofSlopeAndHeightRequestedService
    implements Consumer<DetectionRoofSlopeAndHeightRequested> {
  private final DetectionRepository detectionRepository;
  private final LidarPolygonMetricProcessor lidarPolygonMetricProcessor;
  private static final String ROOF_SLOPE_PROPERTY_NAME = "roof_slope_in_degrees";
  private static final String ROOF_HEIGHT_PROPERTY_NAME = "roof_height_in_meters";

  @Override
  public void accept(DetectionRoofSlopeAndHeightRequested requested) {
    var detection =
        detectionRepository
            .findById(requested.getDetectionId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Detection={" + requested.getDetectionId() + "} not found"));

    var roofDelimitation = detection.getPolygonRoofDelimitation();
    if (roofDelimitation == null) {
      throw new IllegalArgumentException(
          "PolygonRoofDelimitation is null for detection={" + requested.getDetectionId() + "}");
    }

    var roofGeometry = toJtsPolygon(roofDelimitation);

    var dimension = lidarPolygonMetricProcessor.apply(List.of(roofGeometry)).getFirst();
    var roofSlope = dimension.getSlopeInDegrees();
    var roofHeight = dimension.getHeightInMeters();

    saveRoofMetrics(detection, roofSlope, roofHeight);
  }

  private void saveRoofMetrics(Detection detection, double roofSlope, double roofHeight) {
    var providedGeoJsonZone = detection.getDomainProvidedGeoJsonZone();

    if (providedGeoJsonZone == null || providedGeoJsonZone.isEmpty()) {
      return;
    }

    var firstFeature = providedGeoJsonZone.getFirst();
    if (firstFeature.getProperties() == null) {
      firstFeature.setProperties(new HashMap<>());
    }

    firstFeature.getProperties().put(ROOF_SLOPE_PROPERTY_NAME, roofSlope);
    firstFeature.getProperties().put(ROOF_HEIGHT_PROPERTY_NAME, roofHeight);
    detection.setProvidedGeoJsonZone(providedGeoJsonZone);
    detectionRepository.save(detection);
  }

  private static Polygon toJtsPolygon(List<List<BigDecimal>> polygon) {
    var coords =
        polygon.stream()
            .map(
                point -> new Coordinate(point.getFirst().doubleValue(), point.get(1).doubleValue()))
            .toArray(Coordinate[]::new);

    return geometryFactory.createPolygon(coords);
  }
}
