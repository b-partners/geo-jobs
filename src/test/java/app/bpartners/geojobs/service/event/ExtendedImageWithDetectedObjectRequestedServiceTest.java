package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import org.junit.jupiter.api.Test;

class ExtendedImageWithDetectedObjectRequestedServiceTest {
  TileFinder tileFinderMock = mock();
  MachineDetectedTileRepository detectedTileRepositoryMock = mock();
  BucketComponent bucketComponentMock = mock();
  DetectedImageDraw detectedImageDrawMock = mock();
  ExtenderApi extenderApiMock = mock();
  FileWriter fileWriterMock = mock();
  ExtendedImageWithDetectedObjectRequestedService subject =
      new ExtendedImageWithDetectedObjectRequestedService(
          tileFinderMock,
          detectedTileRepositoryMock,
          bucketComponentMock,
          detectedImageDrawMock,
          extenderApiMock,
          fileWriterMock);

  @Test
  void detection_does_not_have_only_geo_json_points() {
    var layers = "cite:PCRS";
    var detectionMock = mock(Detection.class);
    when(detectionMock.getGeoServerProperties())
        .thenReturn(
            new GeoServerProperties().geoServerParameter(new GeoServerParameter().layers(layers)));
    when(detectionMock.hasOnlyPointsGeoJson()).thenReturn(false);

    assertDoesNotThrow(
        () -> subject.accept(new ExtendedImageWithDetectedObjectRequested(detectionMock)));

    verify(detectedTileRepositoryMock, never()).findAllByZdjJobId(any());
    verify(tileFinderMock, never()).getSurroundingTiles(any(), any(), anyInt());
    verify(bucketComponentMock, never()).download(any());
    verify(detectedImageDrawMock, never()).apply(any(), any());
    verify(extenderApiMock, never()).apply(any());
    verify(fileWriterMock, never()).base64ToFile(any(), any());
    verify(bucketComponentMock, never()).upload(any(), any());
  }
}
