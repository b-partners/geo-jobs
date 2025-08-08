package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.lidar.LidarPolygonMetricProcessor;
import app.bpartners.geojobs.service.lidar.model.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DetectionRoofSlopeAndHeightRequestedServiceTest {
  DetectionRepository detectionRepositoryMock = mock();
  LidarPolygonMetricProcessor lidarPolygonMetricProcessorMock = mock();

  DetectionRoofSlopeAndHeightRequestedService subject =
      new DetectionRoofSlopeAndHeightRequestedService(
          detectionRepositoryMock, lidarPolygonMetricProcessorMock);

  @Test
  void save_slope_and_height_ok() {
    var detectionId = "detection-id";
    var expectedRoofSlope = 42.0;
    var expectedRoofHeight = 3.5;
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();

    var detectionMock = detectionMock();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    var dimensionMock = mock(Dimension.class);
    when(dimensionMock.getSlopeInDegrees()).thenReturn(expectedRoofSlope);
    when(dimensionMock.getHeightInMeters()).thenReturn(expectedRoofHeight);
    when(lidarPolygonMetricProcessorMock.apply(anyList()))
        .thenReturn(new ArrayList<>(List.of(dimensionMock)));

    subject.accept(requested);

    var feature = detectionMock.getDomainProvidedGeoJsonZone().getFirst();
    var actualRoofSlope = feature.getProperties().get("roof_slope_in_degrees");
    var actualRoofHeight = feature.getProperties().get("roof_height_in_meters");

    assertEquals(expectedRoofSlope, actualRoofSlope);
    assertEquals(expectedRoofHeight, actualRoofHeight);
    verify(detectionRepositoryMock).save(detectionMock);
  }

  @Test
  void throw_when_detection_not_found() {
    var detectionId = "missing-id";
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();

    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.empty());

    var error = assertThrows(RuntimeException.class, () -> subject.accept(requested));
    assertTrue(error.getMessage().contains("Detection={" + detectionId + "} not found"));
  }

  @Test
  void throw_when_polygon_delimitation_null() {
    var detectionId = "detection-id";
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();
    var detectionMock = mock(Detection.class);

    when(detectionMock.getPolygonRoofDelimitation()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    var error = assertThrows(RuntimeException.class, () -> subject.accept(requested));
    assertTrue(
        error
            .getMessage()
            .contains("PolygonRoofDelimitation is null for detection={" + detectionId + "}"));
  }

  private static Detection detectionMock() {
    var feature = Feature.builder().properties(new java.util.HashMap<>()).build();
    var detection = mock(Detection.class);

    when(detection.getPolygonRoofDelimitation())
        .thenReturn(
            List.of(
                List.of(BigDecimal.ZERO, BigDecimal.ZERO),
                List.of(BigDecimal.ONE, BigDecimal.ZERO),
                List.of(BigDecimal.ONE, BigDecimal.ONE),
                List.of(BigDecimal.ZERO, BigDecimal.ZERO)));

    when(detection.getDomainProvidedGeoJsonZone()).thenReturn(new ArrayList<>(List.of(feature)));
    return detection;
  }
}
