package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toDomainFeature;

import app.bpartners.geojobs.endpoint.event.model.DetectionRoofPropertiesRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.repository.model.detection.RoofCoveringType;
import app.bpartners.geojobs.service.detection.RoofCovering;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DetectionRoofPropertiesRequestedService
    implements Consumer<DetectionRoofPropertiesRequested> {
  private final DetectionRepository detectionRepository;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeometryConverter geometryConverter;
  private final ObjectMapper objectMapper;

  @Override
  public void accept(DetectionRoofPropertiesRequested event) {
    var detectionIdentifier = event.getDetectionIdentifier();
    var detection = detectionRepository.findById(detectionIdentifier).orElseThrow();
    var detectionWithRoofProperties = apply(detection);
    detectionRepository.save(detectionWithRoofProperties);
  }

  public Detection apply(Detection detection) {
    var machineDetectedTiles =
        machineDetectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var featureWithDelimitationsCovering =
        detection.getFeatureWithDelimitations().stream()
            .map(
                featureWithDelimitation -> {
                  var roofFeatures = featureWithDelimitation.getDelimitations();
                  var domainFeaturesWithCovering =
                      roofFeatures.stream()
                          .map(
                              roofFeature -> {
                                var geometryType = roofFeature.getGeometry().getActualInstance();
                                MultiPolygon roofLatLonMultiPolygon;
                                switch (geometryType) {
                                  case Polygon polygon ->
                                      roofLatLonMultiPolygon =
                                          geometryConverter.apply(
                                              List.of(polygon.getCoordinates()));
                                  case app.bpartners.geojobs.endpoint.rest.model.MultiPolygon
                                              multiPolygon ->
                                      roofLatLonMultiPolygon =
                                          geometryConverter.apply(multiPolygon.getCoordinates());
                                  default ->
                                      throw new UnsupportedOperationException(
                                          "Unsupported geometry type for roof: " + geometryType);
                                }
                                Map<RoofCoveringType, Long> summedAreas =
                                    machineDetectedTiles.stream()
                                        .filter(
                                            detectedTile -> {
                                              var tileCoordinates =
                                                  detectedTile.getTile().getCoordinates();
                                              var multiPolygonFromTile =
                                                  geometryConverter.getMultiPolygonFromTile(
                                                      tileCoordinates.getX(),
                                                      tileCoordinates.getY(),
                                                      tileCoordinates.getZ());
                                              return multiPolygonFromTile.intersects(
                                                  roofLatLonMultiPolygon);
                                            })
                                        .map(
                                            detectedTile ->
                                                new DetectedRoofCoveringArea(
                                                    new RoofCovering(
                                                        detectedTile.getPrimaryRoofCoveringType(),
                                                        detectedTile.getPrimaryRoofCoveringArea()),
                                                    new RoofCovering(
                                                        detectedTile.getSecondaryRoofCoveringType(),
                                                        detectedTile
                                                            .getSecondaryRoofCoveringArea())))
                                        .flatMap(d -> Stream.of(d.primary(), d.secondary()))
                                        .collect(
                                            Collectors.groupingBy(
                                                RoofCovering::coating,
                                                Collectors.summingLong(RoofCovering::area)));
                                List<Map.Entry<RoofCoveringType, Long>> sorted =
                                    summedAreas.entrySet().stream()
                                        .sorted(
                                            Map.Entry.<RoofCoveringType, Long>comparingByValue()
                                                .reversed())
                                        .toList();
                                RoofCoveringType primary = null;
                                RoofCoveringType secondary = null;
                                if (!sorted.isEmpty()) {
                                  primary = sorted.getFirst().getKey();
                                }
                                if (sorted.size() > 1) {
                                  secondary = sorted.get(1).getKey();
                                }
                                Map<String, Object> properties =
                                    roofFeature.getProperties() == null
                                        ? new HashMap<>()
                                        : new HashMap<>(roofFeature.getProperties());
                                try {
                                  properties.put(
                                      "covering",
                                      objectMapper.writeValueAsString(
                                          new DetectedRoofCovering(primary, secondary)));
                                } catch (JsonProcessingException e) {
                                  throw new RuntimeException(e);
                                }
                                return toDomainFeature(
                                    new Feature()
                                        .geometry(roofFeature.getGeometry())
                                        .type(roofFeature.getType())
                                        .properties(properties));
                              })
                          .toList();
                  return new FeatureWithDelimitation(
                      featureWithDelimitation.feature(), domainFeaturesWithCovering);
                })
            .toList();
    return detection.toBuilder().featureWithDelimitations(featureWithDelimitationsCovering).build();
  }

  private record DetectedRoofCoveringArea(RoofCovering primary, RoofCovering secondary) {}

  public record DetectedRoofCovering(RoofCoveringType primary, RoofCoveringType secondary) {}
}
