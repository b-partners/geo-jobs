package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.DetectionMaskFromTileRetriever;
import app.bpartners.geojobs.service.TileDetectionTaskConsumer;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TileDetectionTaskConsumerTest {
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  TileObjectDetector objectDetectorMock = mock();
  DetectionMapper detectionMapperMock = mock();
  DetectionRepository detectionRepositoryMock = mock();
  GeometryConverter geometryConverterMock = mock();
  DetectionMaskFromTileRetriever maskRetrieverMock = mock();
  TileDetectionTaskConsumer subject =
      new TileDetectionTaskConsumer(
          machineDetectedTileRepositoryMock,
          objectDetectorMock,
          detectionMapperMock,
          detectionRepositoryMock,
          geometryConverterMock,
          maskRetrieverMock);

  @Test
  void save_machine_detected_tile() {
    var zoneDetectionJobId = randomUUID().toString();
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(new MachineDetectedTile());
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(new DetectionResponse());
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(detectionRepositoryMock.findByZdjId(any())).thenReturn(Optional.empty());

    assertDoesNotThrow(
        () ->
            subject.accept(
                TileDetectionTask.builder()
                    .zoneDetectionJobId(zoneDetectionJobId)
                    .detectableObjectConfigurations(
                        List.of(
                            DetectableObjectConfiguration.builder()
                                .objectType(PASSAGE_PIETON)
                                .build()))
                    .build()));

    var detectedTileCaptor = ArgumentCaptor.forClass(MachineDetectedTile.class);
    verify(machineDetectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
  }

  @Test
  void generate_mask_and_save_machine_detected_tile() {
    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        List.of(mock(DetectableObjectConfiguration.class));
    var zoneDetectionJobId = randomUUID().toString();
    var parcelId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tileMock = mock(Tile.class);
    var detectionMock = mock(Detection.class);
    var featureMock = mock(Feature.class);
    var featureGeometryMock = mock(FeatureGeometry.class);
    var featureMultiPolygonMock = mock(MultiPolygon.class);
    var centroidCoordinates = List.of(BigDecimal.valueOf(0), BigDecimal.valueOf(1));
    var roofMultiPolygonMock = mock(org.locationtech.jts.geom.MultiPolygon.class);
    var maskFileMock = mock(File.class);
    var detectionResponseMock = mock(DetectionResponse.class);
    var machineDetectedTileMock = mock(MachineDetectedTile.class);
    var tileDetectionTask =
        TileDetectionTask.builder()
            .parcelId(parcelId)
            .zoneDetectionJobId(zoneDetectionJobId)
            .detectableObjectConfigurations(detectableObjectConfigurations)
            .tile(tileMock)
            .jobId(parcelJobId)
            .build();

    when(featureGeometryMock.getMultiPolygon()).thenReturn(featureMultiPolygonMock);
    when(featureGeometryMock.getActualInstance()).thenReturn(featureMultiPolygonMock);
    when(featureMock.getGeometry()).thenReturn(featureGeometryMock);
    when(detectionMock.hasToitureModelName()).thenReturn(true);
    when(detectionMock.getProvidedGeoJsonZone()).thenReturn(List.of(featureMock));
    when(detectionRepositoryMock.findByZdjId(zoneDetectionJobId))
        .thenReturn(Optional.of(detectionMock));
    when(geometryConverterMock.centroidFromGeometry(featureMultiPolygonMock))
        .thenReturn(centroidCoordinates);
    when(geometryConverterMock.retrieveNearestRoofMultiPolygon(centroidCoordinates))
        .thenReturn(roofMultiPolygonMock);
    when(machineDetectedTileRepositoryMock.save(any())).thenReturn(machineDetectedTileMock);
    when(objectDetectorMock.apply(any(), any(), any())).thenReturn(detectionResponseMock);
    when(detectionMapperMock.toDetectedTile(any(), any(), any(), any(), any()))
        .thenReturn(new MachineDetectedTile());
    when(maskRetrieverMock.apply(tileMock, roofMultiPolygonMock)).thenReturn(maskFileMock);

    assertDoesNotThrow(() -> subject.accept(tileDetectionTask));

    var detectedTileCaptor = ArgumentCaptor.forClass(MachineDetectedTile.class);
    verify(machineDetectedTileRepositoryMock, times(1)).save(detectedTileCaptor.capture());
    verify(objectDetectorMock)
        .apply(eq(tileDetectionTask), eq(maskFileMock), eq(detectableObjectConfigurations));
    verify(detectionMapperMock)
        .toDetectedTile(
            eq(detectionResponseMock),
            eq(tileMock),
            eq(parcelId),
            eq(zoneDetectionJobId),
            eq(parcelJobId));
  }
}
