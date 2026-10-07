package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.v1.mapper.FeatureMapper.toRestFeature;
import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.model.CustomObjectMapper.objectMapper;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.LIDAR_DATA_STATUS_PROPERTY_NAME;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static app.bpartners.geojobs.service.threed.model.DelimitationType.ENTIRE_ROOF_DELIMITATION;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.FeatureRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.FeatureVggRequested;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.CityJSONThreedProcessor;
import jakarta.persistence.EntityManager;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FeatureRoofSlopeAndHeightRequestedServiceTest {

  DetectionRepository detectionRepositoryMock = mock();
  CityJSONThreedProcessor threedProcessorMock = mock();
  EntityManager entityManagerMock = mock();
  EventProducer eventProducerMock = mock();
  FeatureRoofSlopeAndHeightRequestedService subject =
      new FeatureRoofSlopeAndHeightRequestedService(
          detectionRepositoryMock, threedProcessorMock, eventProducerMock, entityManagerMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).clear();
  }

  @Test
  void save_slope_and_height_ok() {
    var expectedRoofSlope = 42.0;
    var expectedRoofHeight = 3.5;
    var detectionIdentifier = randomUUID().toString();
    var featureIdentifier = randomUUID().toString();

    var domainFeatureProperties =
        new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));
    var delimitationFeatureProperties =
        new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));

    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(domainFeatureProperties)
            .geometry(somePolygon())
            .build();
    var domainFeatureDelimitation =
        Feature.builder().properties(delimitationFeatureProperties).geometry(somePolygon()).build();

    var featureWithDelimitations =
        List.of(
            new FeatureWithDelimitation(
                domainFeatureDelimitation, List.of(domainFeatureDelimitation)));
    var detection =
        Detection.builder()
            .id(detectionIdentifier)
            .featureWithDelimitations(featureWithDelimitations)
            .build();
    when(detectionRepositoryMock.findById(detection.getId())).thenReturn(Optional.of(detection));

    when(threedProcessorMock.generate(any(), any(), eq(ENTIRE_ROOF_DELIMITATION), any()))
        .thenReturn(cityJson(expectedRoofSlope));

    subject.accept(
        FeatureRoofSlopeAndHeightRequested.builder()
            .detectionIdentifier(detection.getId())
            .feature(toRestFeature(domainFeature))
            .build());

    var firstDelimitation =
        detection.getFeatureWithDelimitations().getFirst().delimitations().getFirst();
    var actualRoofSlope = firstDelimitation.getProperties().get(ROOF_SLOPE_PROPERTY_NAME);
    var actualRoofHeight = firstDelimitation.getProperties().get(ROOF_HEIGHT_PROPERTY_NAME);
    var actualRoofDataStatus =
        firstDelimitation.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME);

    var expectedUpdatedProperties = new LinkedHashMap<String, Object>();
    expectedUpdatedProperties.put("feature_id", featureIdentifier);
    expectedUpdatedProperties.put("roof_slope_in_degrees", expectedRoofSlope);
    expectedUpdatedProperties.put("roof_height_in_meters", expectedRoofHeight);
    expectedUpdatedProperties.put("lidar_data_status", AVAILABLE);

    assertEquals(expectedRoofSlope, actualRoofSlope);
    assertEquals(expectedRoofHeight, actualRoofHeight);
    assertEquals(AVAILABLE, actualRoofDataStatus);

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(eventCaptor.capture());
    var featureVggRequested = (FeatureVggRequested) eventCaptor.getValue().getFirst();
    assertEquals(
        new FeatureVggRequested(detectionIdentifier, toRestFeature(domainFeature)),
        featureVggRequested);
  }

  @Test
  void already_processed_detection_should_not_be_processed() {
    var detectionIdentifier = randomUUID().toString();
    var featureIdentifier = randomUUID().toString();
    var featureProperties = new HashMap<String, Object>(Map.of("feature_id", featureIdentifier));
    var domainFeature =
        Feature.builder()
            .id(featureIdentifier)
            .properties(featureProperties)
            .geometry(somePolygon())
            .build();
    var restFeature = toRestFeature(domainFeature);
    var detectionAlreadyProcessed =
        Detection.builder()
            .id(randomUUID().toString())
            .featureWithDelimitations(
                List.of(
                    new FeatureWithDelimitation(
                        domainFeature,
                        List.of(
                            Feature.builder()
                                .geometry(somePolygon())
                                .properties(
                                    new HashMap<>(
                                        Map.of(LIDAR_DATA_STATUS_PROPERTY_NAME, AVAILABLE)))
                                .build()))))
            .build();
    when(detectionRepositoryMock.findById(any()))
        .thenReturn(Optional.of(detectionAlreadyProcessed));

    assertDoesNotThrow(
        () ->
            subject.accept(
                new FeatureRoofSlopeAndHeightRequested(detectionIdentifier, restFeature)));

    verify(threedProcessorMock, never()).generate(any(), any(), any(), any());
    verify(detectionRepositoryMock, never()).save(any());
    verify(eventProducerMock, never()).accept(any());
  }

  /** One roof surface whose highest vertex is at z=10 and one ground surface at z=6.5. */
  @SneakyThrows
  private static File cityJson(double roofSlope) {
    var file = File.createTempFile("cityjson", ".json");
    Files.writeString(
        file.toPath(),
        """
        {
          "type": "CityJSON",
          "transform": {"scale": [0.001, 0.001, 0.001], "translate": [0, 0, 0]},
          "vertices": [[0, 0, 8000], [1000, 0, 8000], [0, 1000, 10000],
                       [0, 0, 6500], [1000, 0, 6500], [0, 1000, 6500]],
          "CityObjects": {
            "0": {
              "type": "Building",
              "geometry": [{
                "type": "MultiSurface",
                "lod": "2",
                "boundaries": [[[0, 1, 2]], [[3, 4, 5]]],
                "semantics": {
                  "surfaces": [
                    {"type": "RoofSurface", "slope_in_degrees": %s, "area_in_square_meters": 10},
                    {"type": "GroundSurface"}
                  ],
                  "values": [0, 1]
                }
              }]
            }
          }
        }
        """
            .formatted(roofSlope));
    return file;
  }

  @SneakyThrows
  Feature.FeatureGeometry somePolygon() {
    return new Feature.FeatureGeometry(
        POLYGON,
        objectMapper()
            .writeValueAsString(
                new app.bpartners.geojobs.endpoint.rest.model.Polygon()
                    .type(app.bpartners.geojobs.endpoint.rest.model.Polygon.TypeEnum.POLYGON)
                    .coordinates(
                        List.of(List.of(List.of(new BigDecimal("0.0"), new BigDecimal("0.0")))))));
  }
}
