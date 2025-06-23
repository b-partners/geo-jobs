package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetectionAreaValidatorTest {
  GeometrySquareMeterArea geometrySquareMeterArea = mock(GeometrySquareMeterArea.class);
  GeometryConverter geometryConverter = mock(GeometryConverter.class);
  Detection detectionMock;
  FeatureGeometry featureGeometryMock;
  Feature mockFeature;
  GeoServerParameter geoServerParameterMock;
  GeoServerProperties geoServerPropertiesMock;

  DetectionAreaValidator subject;

  @BeforeEach
  void setUp() {
    detectionMock = mock(Detection.class);
    mockFeature = mock(Feature.class);
    geoServerParameterMock = mock(GeoServerParameter.class);
    featureGeometryMock = mock(FeatureGeometry.class);
    geoServerPropertiesMock = mock(GeoServerProperties.class);
    subject = new DetectionAreaValidator(geometrySquareMeterArea, geometryConverter);
  }

  @Test
  void accept_UnsupportedOperationException() {
    var unsupportedGeometryType = new Object();

    when(featureGeometryMock.getActualInstance()).thenReturn(unsupportedGeometryType);
    when(mockFeature.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getGeoJsonZone()).thenReturn(List.of(mockFeature));
    when(geoServerParameterMock.getLayers()).thenReturn("layer");
    when(geoServerPropertiesMock.getGeoServerParameter()).thenReturn(geoServerParameterMock);
    when(detectionMock.getGeoServerProperties()).thenReturn(geoServerPropertiesMock);

    var actual =
        assertThrows(UnsupportedOperationException.class, () -> subject.accept(detectionMock));

    assertTrue(actual.getMessage().contains("Unsupported geometry type for validation:"));
  }

  @Test
  void accept_IllegalStateException() {
    var pointMock = mock(Point.class);
    when(featureGeometryMock.getActualInstance()).thenReturn(pointMock);
    when(mockFeature.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getGeoJsonZone()).thenReturn(List.of(mockFeature));
    when(geoServerParameterMock.getLayers()).thenReturn("layer");
    when(geoServerPropertiesMock.getGeoServerParameter()).thenReturn(geoServerParameterMock);
    when(detectionMock.getGeoServerProperties()).thenReturn(geoServerPropertiesMock);

    var actual = assertThrows(IllegalStateException.class, () -> subject.accept(detectionMock));

    assertEquals("Unable to unify provided multiPolygon", actual.getMessage());
  }

  @Test
  void accept_NotImplementedException() {
    var polygonMock = mock(Polygon.class);
    var multiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var INDRE_ET_LOIRE_2024_5_CM = "INDRE_ET_LOIRE_2024_5CM";
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)),
                List.of(BigDecimal.valueOf(3.0), BigDecimal.valueOf(4.0))));
    when(polygonMock.getCoordinates()).thenReturn(coordinates);
    when(featureGeometryMock.getActualInstance()).thenReturn(polygonMock);
    when(mockFeature.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getGeoJsonZone()).thenReturn(List.of(mockFeature));
    when(geoServerParameterMock.getLayers()).thenReturn(INDRE_ET_LOIRE_2024_5_CM);
    when(geoServerPropertiesMock.getGeoServerParameter()).thenReturn(geoServerParameterMock);
    when(detectionMock.getGeoServerProperties()).thenReturn(geoServerPropertiesMock);
    when(geometryConverter.apply(any())).thenReturn(multiPolygonMock);
    when(geometrySquareMeterArea.apply(multiPolygonMock)).thenReturn(15_000.0);

    var actual = assertThrows(NotImplementedException.class, () -> subject.accept(detectionMock));
    assertEquals(
        "Provided multiPolygon must be under 10 000 meters for zone inside layers "
            + INDRE_ET_LOIRE_2024_5_CM
            + " for now",
        actual.getMessage());
  }

  @Test
  void accept_detection_area_validator_ok() {
    var polygonMock = mock(Polygon.class);
    var multiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var INDRE_ET_LOIRE_2024_5_CM = "INDRE_ET_LOIRE_2024_5CM";
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)),
                List.of(BigDecimal.valueOf(3.0), BigDecimal.valueOf(4.0))));
    when(polygonMock.getCoordinates()).thenReturn(coordinates);
    when(featureGeometryMock.getActualInstance()).thenReturn(polygonMock);
    when(mockFeature.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getGeoJsonZone()).thenReturn(List.of(mockFeature));
    when(geoServerParameterMock.getLayers()).thenReturn(INDRE_ET_LOIRE_2024_5_CM);
    when(geoServerPropertiesMock.getGeoServerParameter()).thenReturn(geoServerParameterMock);
    when(detectionMock.getGeoServerProperties()).thenReturn(geoServerPropertiesMock);
    when(geometryConverter.apply(any())).thenReturn(multiPolygonMock);
    when(geometrySquareMeterArea.apply(multiPolygonMock)).thenReturn(10_000.0);

    assertDoesNotThrow(() -> subject.accept(detectionMock));

    verify(polygonMock).getCoordinates();
    verify(geometryConverter).apply(any());
    verify(geometrySquareMeterArea).apply(multiPolygonMock);
    verify(geoServerParameterMock).getLayers();
  }
}
