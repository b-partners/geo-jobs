package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.toRestFeature;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.TileExtendedImageRequested;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import app.bpartners.geojobs.service.event.TileExtendedImageRequestedService;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.mockito.MockedStatic;

class PointExtendedImageRequestTest {
  EventProducer eventProducerMock;
  TileExtendedImageRequestedService tileExtendedImageRequestedServiceMock;
  GeometryConverter geometryConverterMock;
  GeometryTiledValidator geometryTiledValidatorMock;
  DetectionBackgroundRetriever detectionBackgroundRetrieverMock;
  PointExtendedImageRequest subject;
  Feature featureMock;
  FeatureGeometry featureGeometryMock;
  Detection detectionMock;

  @BeforeEach
  void setup() {
    featureMock = mock(Feature.class);
    featureGeometryMock = mock(FeatureGeometry.class);
    geometryTiledValidatorMock = mock(GeometryTiledValidator.class);
    detectionMock = mock(Detection.class);
    tileExtendedImageRequestedServiceMock = mock(TileExtendedImageRequestedService.class);
    eventProducerMock = mock(EventProducer.class);
    geometryConverterMock = mock(GeometryConverter.class);
    detectionBackgroundRetrieverMock = mock(DetectionBackgroundRetriever.class);
    subject =
        new PointExtendedImageRequest(
            eventProducerMock,
            tileExtendedImageRequestedServiceMock,
            geometryConverterMock,
            geometryTiledValidatorMock);
  }

  @Test
  void accept_geometryTiledValidator_equals_false() {
    var pointMock = mock(Point.class);

    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(pointMock);
    when(geometryTiledValidatorMock.apply(pointMock)).thenReturn(false);

    subject.accept(detectionMock, featureMock, "layer", true);

    verify(geometryTiledValidatorMock).apply(pointMock);
    verifyNoInteractions(
        tileExtendedImageRequestedServiceMock, eventProducerMock, geometryConverterMock);
  }

  @Test
  void accept_throws_IllegalArgumentException_geometry_type() {
    var detectionId = randomUUID().toString();
    var geometryMock = mock(org.locationtech.jts.geom.Geometry.class);
    var objectGeometry = new Object();
    var delimitationFeatureMock = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var delimitationFeatureRestMock = mock(app.bpartners.geojobs.endpoint.rest.model.Feature.class);
    var delimitationFeatureGeometryMock = mock(FeatureGeometry.class);
    var featureWithDelimitation =
        new FeatureWithDelimitation(null, List.of(delimitationFeatureMock));

    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(geometryMock);
    when(geometryTiledValidatorMock.apply(any())).thenReturn(true);
    when(geometryConverterMock.centroidFromGeometry(geometryMock))
        .thenReturn(List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0)));
    when(delimitationFeatureRestMock.getGeometry()).thenReturn(delimitationFeatureGeometryMock);
    when(delimitationFeatureGeometryMock.getActualInstance()).thenReturn(objectGeometry);
    when(detectionMock.getFeatureWithDelimitations()).thenReturn(List.of(featureWithDelimitation));
    when(detectionMock.getId()).thenReturn(detectionId);

    try (MockedStatic<FeatureMapper> featureMapperMockedStatic = mockStatic(FeatureMapper.class)) {
      featureMapperMockedStatic
          .when(() -> toRestFeature(delimitationFeatureMock))
          .thenReturn(delimitationFeatureRestMock);

      var actual =
          assertThrows(
              IllegalArgumentException.class,
              () -> subject.accept(detectionMock, featureMock, "layer", true));

      assertEquals(
          "Unsupported geometry type to extended image: " + objectGeometry, actual.getMessage());

      verify(geometryTiledValidatorMock).apply(geometryMock);
      verify(geometryConverterMock).centroidFromGeometry(geometryMock);
      verify(tileExtendedImageRequestedServiceMock, never()).accept(any());
      verify(eventProducerMock, never()).accept(any());
    }
  }

  @Test
  void accept_throws_IllegalStateException() {
    var detectionId = randomUUID().toString();
    var geometryMock = mock(org.locationtech.jts.geom.Geometry.class);
    var featureWithDelimitation = new FeatureWithDelimitation(null, List.of());

    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(geometryMock);
    when(geometryTiledValidatorMock.apply(geometryMock)).thenReturn(true);
    when(geometryConverterMock.centroidFromGeometry(geometryMock))
        .thenReturn(List.of(BigDecimal.valueOf(10), BigDecimal.valueOf(20)));
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getFeatureWithDelimitations()).thenReturn(List.of(featureWithDelimitation));

    var actual =
        assertThrows(
            IllegalStateException.class,
            () -> subject.accept(detectionMock, featureMock, "layer", true));

    assertEquals(
        "Unable to unify delimitation multiPolygon for detection.id: " + detectionId,
        actual.getMessage());

    verify(tileExtendedImageRequestedServiceMock, never()).accept(any());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void accept_isSynchronous_ok() {
    var detectionId = randomUUID().toString();
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var geometryMock = mock(org.locationtech.jts.geom.Geometry.class);
    var domaineFeatureMock = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var restFeatureMock = mock(app.bpartners.geojobs.endpoint.rest.model.Feature.class);
    var delimitationFeatureGeometryMock = mock(FeatureGeometry.class);
    var delimitationPolygonMock = mock(app.bpartners.geojobs.endpoint.rest.model.Polygon.class);
    var polygonCoordinates =
        List.of(
            List.of(
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(0)),
                List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(1)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(1)),
                List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(0))));
    var expectedInputToGeometryConverter = Collections.singletonList(polygonCoordinates);
    var featureWithDelimitation = new FeatureWithDelimitation(null, List.of(domaineFeatureMock));
    var detectionMock = mock(app.bpartners.geojobs.repository.model.detection.Detection.class);
    var unifiedRoofMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);

    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(geometryMock);
    when(geometryTiledValidatorMock.apply(geometryMock)).thenReturn(true);
    when(geometryConverterMock.centroidFromGeometry(geometryMock))
        .thenReturn(List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0)));
    when(restFeatureMock.getGeometry()).thenReturn(delimitationFeatureGeometryMock);
    when(delimitationFeatureGeometryMock.getActualInstance()).thenReturn(delimitationPolygonMock);
    when(delimitationPolygonMock.getCoordinates()).thenReturn(polygonCoordinates);
    when(detectionMock.getFeatureWithDelimitations()).thenReturn(List.of(featureWithDelimitation));
    when(detectionMock.getId()).thenReturn(detectionId);
    when(geometryConverterMock.apply(expectedInputToGeometryConverter))
        .thenReturn(unifiedRoofMultiPolygonMock);

    try (MockedStatic<app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper>
        featureMapperMockedStatic =
            mockStatic(app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.class)) {
      featureMapperMockedStatic
          .when(() -> toRestFeature(domaineFeatureMock))
          .thenReturn(restFeatureMock);

      subject.accept(detectionMock, featureMock, "layer", true);

      verify(featureMock, times(1)).getGeometry();
      verify(featureGeometryMock, times(1)).getActualInstance();
      verify(detectionMock, times(1)).getFeatureWithDelimitations();
      verify(eventProducerMock, never()).accept(anyList());
      verify(geometryTiledValidatorMock, times(1)).apply(geometryMock);
      verify(geometryConverterMock, times(1)).centroidFromGeometry(geometryMock);
      verify(restFeatureMock, times(1)).getGeometry();
      verify(delimitationFeatureGeometryMock, times(1)).getActualInstance();
      verify(delimitationPolygonMock, times(1)).getCoordinates();
    }
  }

  @Test
  void accept_processes_ok() {
    var detectionId = randomUUID().toString();
    var geometryMock = mock(org.locationtech.jts.geom.Geometry.class);
    var delimitationFeatureGeometryMock = mock(FeatureGeometry.class);
    var delimitationFeatureRestMock = mock(app.bpartners.geojobs.endpoint.rest.model.Feature.class);
    var delimitationFeatureMock = mock(app.bpartners.geojobs.repository.model.Feature.class);
    var polygonMock = mock(app.bpartners.geojobs.endpoint.rest.model.Polygon.class);
    var mockMultiPolygonMock = mock(MultiPolygon.class);
    var featureWithDelimitation =
        new FeatureWithDelimitation(null, List.of(delimitationFeatureMock));

    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(geometryMock);
    when(geometryTiledValidatorMock.apply(geometryMock)).thenReturn(true);
    when(geometryConverterMock.centroidFromGeometry(geometryMock))
        .thenReturn(List.of(BigDecimal.valueOf(10), BigDecimal.valueOf(20)));
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getFeatureWithDelimitations()).thenReturn(List.of(featureWithDelimitation));

    try (MockedStatic<FeatureMapper> featureMapperMockedStatic = mockStatic(FeatureMapper.class)) {
      featureMapperMockedStatic
          .when(() -> toRestFeature(delimitationFeatureMock))
          .thenReturn(delimitationFeatureRestMock);

      when(delimitationFeatureRestMock.getGeometry()).thenReturn(delimitationFeatureGeometryMock);
      when(delimitationFeatureGeometryMock.getActualInstance()).thenReturn(polygonMock);
      when(geometryConverterMock.apply(any())).thenReturn(mockMultiPolygonMock);

      subject.accept(detectionMock, featureMock, "layerName", true);

      verify(tileExtendedImageRequestedServiceMock, times(1))
          .accept(any(TileExtendedImageRequested.class));
      verify(eventProducerMock, never()).accept(any());
    }
  }
}
