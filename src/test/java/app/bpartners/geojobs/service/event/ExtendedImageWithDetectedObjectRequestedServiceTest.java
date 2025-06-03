package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE_CLAIR;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.PolygonObjectTypeSerializable;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygonSerializable;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.NoSuchElementException;
import java.util.Optional;
import app.bpartners.geojobs.service.tiling.TiledPixelPolygonFilter;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExtendedImageWithDetectedObjectRequestedServiceTest {
  TileFinder tileFinderMock = mock();
  MachineDetectedTileRepository detectedTileRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  DetectedImageDraw detectedImageDrawMock = mock();
  ExtenderApi extenderApiMock = mock();
  FileWriter fileWriterMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  GeometryConverter geometryConverterMock = mock();
  EventProducer eventProducerMock = mock();
  DetectionVGGRequestedService detectionVGGRequestedServiceMock = mock();
  ExtendedImageWithDetectedObjectRequestedService subject =
      new ExtendedImageWithDetectedObjectRequestedService(
          tileFinderMock,
          detectedTileRepositoryMock,
          bucketComponentMock,
          detectedImageDrawMock,
          extenderApiMock,
          fileWriterMock,
          detectionRepositoryMock,
          geometryConverterMock,
          eventProducerMock,
          detectionVGGRequestedServiceMock);

  @Test
  void detection_not_found() {
    when(detectionRepositoryMock.findById(any())).thenReturn(Optional.empty());
    boolean isSynchronous = false;

    assertThrows(
        NoSuchElementException.class,
        () ->
            subject.accept(
                new ExtendedImageWithDetectedObjectRequested(
                    randomUUID().toString(), isSynchronous)));
  }

  @Test
  void detection_does_not_have_point_delimitation() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.getPointDelimitation()).thenReturn(null);
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
  void
      detection_with_point_delimitation_convert_tiled_pixel_polygon_and_produces_detection_vgg_requested() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var zdjId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    var providedFeaturePointMock = mock(Feature.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var pointMock = mock(Point.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var tileCoordinatesMock = mock(TileCoordinates.class);
    var tileMock = mock(Tile.class);
    var detectedObjectMock = mock(DetectedObject.class);
    var featureToitureMultiPolygon = mock(Feature.class);
    var longitude = BigDecimal.valueOf(0);
    var latitude = BigDecimal.valueOf(1);
    int xTile = 0;
    int yTile = 0;
    int z = 20;
    var featureDetectedObjectMock = mock(Feature.class);
    var detectedFeatureGeometryMock = mock(FeatureGeometry.class);
    var detectedMultiPolygonMock = mock(MultiPolygon.class);
    var xPixel = BigDecimal.valueOf(0);
    var yPixel = BigDecimal.valueOf(0);
    var detectedPolygonMock = mock(org.locationtech.jts.geom.Polygon.class);
    HashMap<Feature, Feature> pointDelimitation = new HashMap<>();
    var toitureConvertedMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var featureGeometryToitureMultiPolygonMock = mock(FeatureGeometry.class);
    var toitureMultiPolygonMock = mock(MultiPolygon.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(true);
    when(detectionMock.getZdjId()).thenReturn(zdjId);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(providedFeaturePointMock));
    when(detectionMock.getPointDelimitation()).thenReturn(pointDelimitation);

    when(tileCoordinatesMock.getX()).thenReturn(xTile);
    when(tileCoordinatesMock.getY()).thenReturn(yTile);
    when(tileCoordinatesMock.getZ()).thenReturn(z);

    when(detectedFeatureGeometryMock.getMultiPolygon()).thenReturn(detectedMultiPolygonMock);
    when(featureDetectedObjectMock.getGeometry()).thenReturn(detectedFeatureGeometryMock);
    when(detectedObjectMock.getFeature()).thenReturn(featureDetectedObjectMock);
    when(detectedObjectMock.getDetectableObjectType()).thenReturn(MOISISSURE_CLAIR);

    when(tileMock.getCoordinates()).thenReturn(tileCoordinatesMock);
    when(machineDetectedTileMock.getTile()).thenReturn(tileMock);
    when(machineDetectedTileMock.getDetectedObjects()).thenReturn(List.of(detectedObjectMock));

    when(pointMock.getCoordinates()).thenReturn(List.of(longitude, latitude));
    when(featureGeometryMock.getPoint()).thenReturn(pointMock);
    when(providedFeaturePointMock.getGeometry()).thenReturn(featureGeometryMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(pointMock);
    when(featureGeometryToitureMultiPolygonMock.getActualInstance()).thenReturn(toitureMultiPolygonMock);
    when(featureGeometryToitureMultiPolygonMock.getMultiPolygon())
        .thenReturn(toitureMultiPolygonMock);
    when(featureToitureMultiPolygon.getGeometry())
        .thenReturn(featureGeometryToitureMultiPolygonMock);
    pointDelimitation.put(providedFeaturePointMock, featureToitureMultiPolygon);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));
    when(detectedTileRepositoryMock.findAllByZdjJobId(zdjId))
        .thenReturn(List.of(machineDetectedTileMock));
    when(tileFinderMock.getSurroundingTiles(eq(longitude), eq(latitude), eq(z)))
        .thenReturn(List.of(tileCoordinatesMock));
    when(geometryConverterMock.toPolygon(any())).thenReturn(detectedPolygonMock);
    when(geometryConverterMock.apply(any())).thenReturn(toitureConvertedMultiPolygonMock);
    when(tiledPixelPolygonFilterMock.filterPolygonsInMask(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var multiPolygonStringValue = "multiPolygonStringValue";
    when(geometryConverterMock.writeGeometryAsString(any())).thenReturn(multiPolygonStringValue);
    when(bucketComponentMock.download(any())).thenReturn(mock(File.class));
    when(geometryConverterMock.polygonToPoints(any())).thenReturn(List.of(List.of(xPixel, yPixel)));
    when(detectedImageDrawMock.apply(any(), any())).thenReturn(mock(File.class));
    assertDoesNotThrow(
        () -> subject.accept(new ExtendedImageWithDetectedObjectRequested(detectionId)));
    verify(detectedTileRepositoryMock, times(1)).findAllByZdjJobId(zdjId);
    verify(geometryConverterMock, times(1)).apply(any());
    verify(tileFinderMock, times(1)).getSurroundingTiles(any(), any(), anyInt());
    verify(tiledPixelPolygonFilterMock, times(1)).filterPolygonsInMask(any(), any());
    verify(geometryConverterMock, times(1)).writeGeometryAsString(any());
    verify(extenderApiMock, times(1)).apply(any());
    verify(fileWriterMock, times(1)).base64ToFile(any(), any());
    verify(bucketComponentMock).download(any());
    verify(bucketComponentMock).upload(any(), any());
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    DetectionVGGRequested event = (DetectionVGGRequested) listCaptor.getValue().getFirst();
    assertEquals(
        expectedVggRequested(
            detectionId, providedFeaturePointMock, multiPolygonStringValue, xTile, yTile, z),
        event);
  }

  private @NotNull DetectionVGGRequested expectedVggRequested(
      String detectionId,
      Feature providedFeaturePointMock,
      String multiPolygonStringValue,
      int xTile,
      int yTile,
      int z) {
    return new DetectionVGGRequested(
        detectionId,
        List.of(
            new TiledPixelPolygonSerializable(
                providedFeaturePointMock,
                List.of(
                    new PolygonObjectTypeSerializable(multiPolygonStringValue, MOISISSURE_CLAIR)),
                xTile,
                yTile,
                z)));
  }

  @Test
  void detection_with_null_point_delimitation_skip() {
    var layers = "cite:PCRS";
    var detectionId = randomUUID().toString();
    var zdjId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(true);
    when(detectionMock.getZdjId()).thenReturn(zdjId);

    when(detectionMock.getPointDelimitation()).thenReturn(null);
    when(detectionRepositoryMock.findById(detectionId)).thenReturn(Optional.of(detectionMock));

    assertDoesNotThrow(
        () -> subject.accept(new ExtendedImageWithDetectedObjectRequested(detectionId)));

    verify(detectedTileRepositoryMock, never()).findAllByZdjJobId(any());
    verify(geometryConverterMock, never()).apply(any());
    verify(tileFinderMock, never()).getSurroundingTiles(any(), any(), anyInt());
    verify(tiledPixelPolygonFilterMock, never()).filterPolygonsInMask(any(), any());
    verify(geometryConverterMock, never()).writeGeometryAsString(any());
    verify(eventProducerMock, never()).accept(any());
    verify(bucketComponentMock, never()).download(any());
    verify(extenderApiMock, never()).apply(any());
    verify(fileWriterMock, never()).base64ToFile(any(), any());
    verify(bucketComponentMock, never()).upload(any(), any());
  }
}
