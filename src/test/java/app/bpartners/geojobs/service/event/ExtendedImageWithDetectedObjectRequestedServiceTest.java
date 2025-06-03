package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
          eventProducerMock);

  @Test
  void detection_not_found() {
    when(detectionRepositoryMock.findById(any())).thenReturn(Optional.empty());

    assertThrows(
        NoSuchElementException.class,
        () ->
            subject.accept(new ExtendedImageWithDetectedObjectRequested(randomUUID().toString())));
  }
}
