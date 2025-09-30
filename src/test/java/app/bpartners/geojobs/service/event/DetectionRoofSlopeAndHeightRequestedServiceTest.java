package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.event.DetectionRoofSlopeAndHeightRequestedService.*;
import static app.bpartners.geojobs.service.lidar.model.LidarDataStatus.AVAILABLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor;
import app.bpartners.geojobs.service.lidar.LidarRoofsAnalysisProcessor.RoofsAnalysisResult;
import app.bpartners.geojobs.service.lidar.model.roof.LidarRoofData;
import app.bpartners.geojobs.service.lidar.model.roof.RoofProperties;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

class DetectionRoofSlopeAndHeightRequestedServiceTest {
  FeatureMapper featureMapperMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  LidarRoofsAnalysisProcessor lidarRoofsAnalysisProcessorMock = mock();
  ZoneVggRequestedService zoneVggRequestedServiceMock = mock();
  EntityManager entityManagerMock = mock();

  DetectionRoofSlopeAndHeightRequestedService subject =
      new DetectionRoofSlopeAndHeightRequestedService(
          detectionRepositoryMock,
          lidarRoofsAnalysisProcessorMock,
          featureMapperMock,
          entityManagerMock,
          zoneVggRequestedServiceMock);

  @BeforeEach
  void setUp() {
    doNothing().when(entityManagerMock).clear();
    doNothing().when(zoneVggRequestedServiceMock).accept(any());
  }

  @Test
  void save_slope_and_height_ok() {
    var detectionId = "detection-id";
    var expectedRoofSlope = 42.0;
    var expectedRoofHeight = 3.5;
    var requested = DetectionRoofSlopeAndHeightRequested.builder().detectionId(detectionId).build();

    var detection = detection();
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detection));
    when(featureMapperMock.domainToGeometry(any())).thenReturn(mock(Polygon.class));

    var data = mock(LidarRoofData.class);
    var result = mock(RoofsAnalysisResult.class);
    var properties = mock(RoofProperties.class);

    when(data.status()).thenReturn(AVAILABLE);
    when(properties.getData()).thenReturn(data);
    when(result.getProperties(any())).thenReturn(properties);
    when(properties.getSlopeInDegree()).thenReturn(expectedRoofSlope);
    when(properties.getHeightInMeter()).thenReturn(expectedRoofHeight);
    when(lidarRoofsAnalysisProcessorMock.apply(anySet())).thenReturn(result);

    subject.accept(requested);

    var firstDelimitation =
        detection.getFeatureWithDelimitations().getFirst().delimitations().getFirst();
    var actualRoofSlope = firstDelimitation.getProperties().get(ROOF_SLOPE_PROPERTY_NAME);
    var actualRoofHeight = firstDelimitation.getProperties().get(ROOF_HEIGHT_PROPERTY_NAME);
    var actualRoofDataStatus =
        firstDelimitation.getProperties().get(LIDAR_DATA_STATUS_PROPERTY_NAME);

    assertEquals(expectedRoofSlope, actualRoofSlope);
    assertEquals(expectedRoofHeight, actualRoofHeight);
    assertEquals(AVAILABLE, actualRoofDataStatus);
    verify(detectionRepositoryMock).save(detection);
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

    when(detectionMock.getFeatureWithDelimitations()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    var error = assertThrows(RuntimeException.class, () -> subject.accept(requested));
    assertTrue(
        error
            .getMessage()
            .contains("FeatureWithDelimitation is null for detection={" + detectionId + "}"));
  }

  private static Detection detection() {
    var feature = Feature.builder().properties(new HashMap<>()).build();
    var featureWithDelimitations = List.of(new FeatureWithDelimitation(feature, List.of(feature)));
    return Detection.builder().featureWithDelimitations(featureWithDelimitations).build();
  }
}
