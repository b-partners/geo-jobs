package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

class DetectionProvidedZoneUnifierTest {
  GeometryConverter geometryConverterMock;
  DetectionProvidedZoneUnifier subject;
  FeatureGeometry featureGeometryMock;
  Feature featureMock;
  Detection detectionMock;

  @BeforeEach
  void setup() {
    geometryConverterMock = mock(GeometryConverter.class);
    featureGeometryMock = mock(FeatureGeometry.class);
    featureMock = mock(Feature.class);
    detectionMock = mock(Detection.class);
    subject = new DetectionProvidedZoneUnifier(geometryConverterMock);
  }

  @Test
  void apply_polygon_ok() {
    var detectionID = randomUUID().toString();
    var polygonMock = mock(Polygon.class);
    var multiPolygonMock = mock(MultiPolygon.class);
    var coordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)),
                List.of(BigDecimal.valueOf(3.0), BigDecimal.valueOf(4.0))));

    when(polygonMock.getCoordinates()).thenReturn(coordinates);
    when(featureGeometryMock.getActualInstance()).thenReturn(polygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(detectionMock.getId()).thenReturn(detectionID);
    when(geometryConverterMock.apply(any())).thenReturn(multiPolygonMock);

    var result = subject.apply(detectionMock);

    assertNotNull(result);
    assertEquals(multiPolygonMock, result);
  }

  @Test
  void apply_UnsupportedOperationException() {
    var detectionID = randomUUID().toString();
    when(featureGeometryMock.getActualInstance()).thenReturn(new Object());
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(detectionMock.getId()).thenReturn(detectionID);

    var actual =
        assertThrows(UnsupportedOperationException.class, () -> subject.apply(detectionMock));

    assertTrue(
        actual
            .getMessage()
            .contains(
                "Unsupported geometry type to retrieve multiPolygon for"
                    + " tileDetectionTask : "
                    + featureMock.getGeometry().getActualInstance()));
  }

  @Test
  void apply_IllegalArgumentException() {
    var detectionID = randomUUID().toString();
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of());
    when(detectionMock.getId()).thenReturn(detectionID);

    var actual = assertThrows(IllegalArgumentException.class, () -> subject.apply(detectionMock));

    assertTrue(
        actual
            .getMessage()
            .contains("Unable to unify provided zone for detection.id : " + detectionMock.getId()));
  }
}
