package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import app.bpartners.geojobs.service.tiling.TiledPixelPolygonFilter;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Test;

class ExtendedImageWithDetectedObjectRequestedServiceTest {
  TileFinder tileFinderMock = mock();
  MachineDetectedTileRepository detectedTileRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  DetectedImageDraw detectedImageDrawMock = mock();
  ExtenderApi extenderApiMock = mock();
  FileWriter fileWriterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  TiledPixelPolygonFilter tiledPixelPolygonFilterMock = mock();
  GeometryConverter geometryConverterMock = mock();
  EventProducer eventProducerMock = mock();
  ExtendedImageWithDetectedObjectRequestedService subject =
      new ExtendedImageWithDetectedObjectRequestedService(
          tileFinderMock,
          detectedTileRepositoryMock,
          bucketComponentMock,
          detectedImageDrawMock,
          extenderApiMock,
          fileWriterMock,
          detectionRepositoryMock,
          tiledPixelPolygonFilterMock,
          geometryConverterMock,
          eventProducerMock);

  @Test
  void detection_not_found() {
    when(detectionRepositoryMock.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () ->
            subject.accept(new ExtendedImageWithDetectedObjectRequested(randomUUID().toString())));
  }

  @Test
  void detection_does_not_have_only_geo_json_points() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(false);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
        () -> subject.accept(new ExtendedImageWithDetectedObjectRequested(detectionId)));

    verify(detectedTileRepositoryMock, never()).findAllByZdjJobId(any());
    verify(tileFinderMock, never()).getSurroundingTiles(any(), any(), anyInt());
    verify(bucketComponentMock, never()).download(any());
    verify(detectedImageDrawMock, never()).apply(any(), any());
    verify(extenderApiMock, never()).apply(any());
    verify(fileWriterMock, never()).base64ToFile(any(), any());
    verify(bucketComponentMock, never()).upload(any(), any());
  }

  @Test
  void throwException_when_detectionNotFound() {
    var detectionId = randomUUID().toString();
    ExtendedImageWithDetectedObjectRequested event =
            new ExtendedImageWithDetectedObjectRequested(detectionId);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.empty());

    var actual = assertThrows(
            NoSuchElementException.class,
            () -> subject.accept(event)
    );

    assertEquals("Exception", actual.getMessage());
  }

  @Test
  void detection_has_no_point_delimitation() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties())
            .thenReturn(
                    new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(true);
    when(detectionMock.getPointDelimitation()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
            () -> subject.accept(new ExtendedImageWithDetectedObjectRequested(detectionId)));

    verify(tileFinderMock, never()).getSurroundingTiles(any(), any(), anyInt());
    verify(detectedTileRepositoryMock, never()).findAllByZdjJobId(any());
  }

  @Test
  void processus_complet() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var zdjId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties()).thenReturn(new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(true);
    when(detectionMock.getZdjId()).thenReturn(zdjId);

    /// infos du GEOJSON ///
    var geoJsonPoint = List.of(BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0));
    var geoJsonPointFeature = mock(Point.class);
    when(geoJsonPointFeature.getCoordinates()).thenReturn(geoJsonPoint);
    var mockedgeoJsonFeatureGeometry = mock(FeatureGeometry.class);
    when(mockedgeoJsonFeatureGeometry.getPoint()).thenReturn(geoJsonPointFeature);
    when(mockedgeoJsonFeatureGeometry.getActualInstance()).thenReturn(geoJsonPointFeature);

    var feature = new Feature().geometry(mockedgeoJsonFeatureGeometry);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(feature));

    /// infos Feature Objet Json ///
    var featurePointDelimitation = List.of(BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0));
    var featureMockedpointDelimitation = mock(Point.class);
    when(featureMockedpointDelimitation.getCoordinates()).thenReturn(featurePointDelimitation);

    var mockedFeatureGeometry = mock(FeatureGeometry.class);
    when(mockedFeatureGeometry.getPoint()).thenReturn(featureMockedpointDelimitation);
    when(mockedFeatureGeometry.getActualInstance()).thenReturn(featureMockedpointDelimitation);

    var tileCoordinates = new TileCoordinates().x(1).y(2).z(HOUSES_0.getZoomLevel());

  }
}
