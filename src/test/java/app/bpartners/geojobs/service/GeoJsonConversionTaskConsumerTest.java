package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.POLYGON;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedTile;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeoJsonConversionTaskConsumerTest {
  private static final String ZONE_DETECTION_JOB_ID = "zoneDetectionJobId";
  private static final String CONVERSION_JOB_ID = "conversionJobId";
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  HumanDetectedTileRepository humanDetectedTileRepositoryMock = mock();
  GeoJsonConversionJobRepository geoJsonConversionJobRepositoryMock = mock();
  GeoJsonConverter geoJsonConverterMock = mock();
  BucketComponent bucketComponentMock = mock();
  FileWriter fileWriterMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  GeoJsonConversionTaskConsumer subject =
      new GeoJsonConversionTaskConsumer(
          machineDetectedTileRepositoryMock,
          humanDetectedTileRepositoryMock,
          geoJsonConversionJobRepositoryMock,
          geoJsonConverterMock,
          bucketComponentMock,
          fileWriterMock,
          zoneDetectionJobServiceMock);

  @Test
  void consumes_human_detected_tiles_without_exceptions() {
    when(geoJsonConversionJobRepositoryMock.findById(any()))
        .thenReturn(
            Optional.of(
                GeoJsonConversionJob.builder()
                    .id(CONVERSION_JOB_ID)
                    .zoneDetectionJobType(HUMAN)
                    .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
                    .build()));
    when(zoneDetectionJobServiceMock.findById(ZONE_DETECTION_JOB_ID))
        .thenReturn(
            ZoneDetectionJob.builder().id(ZONE_DETECTION_JOB_ID).zoneName("dummyZoneName").build());
    when(humanDetectedTileRepositoryMock.findAllByJobId(any(), any()))
        .thenReturn(List.of(humanDetectedTile()));
    when(geoJsonConverterMock.convert(any())).thenReturn(new GeoJson(List.of()));
    when(fileWriterMock.writeAsByte(any())).thenReturn(null);
    when(fileWriterMock.write(any(), any(), any())).thenReturn(mock(File.class));
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionTask.builder().page(1).jobId(CONVERSION_JOB_ID).build()));
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(humanDetectedTileRepositoryMock, only()).findAllByJobId(any(), any());
    verify(machineDetectedTileRepositoryMock, never()).findAllByZdjJobId(any(), any());
    verify(geoJsonConverterMock, only()).convert(listCaptor.capture());
    var detectedTile = (DetectedTile) listCaptor.getValue().getFirst();
    assertEquals(expectedDetectedTile(), detectedTile);
  }

  @Test
  void consumes_machine_detected_tiles_without_exceptions() {
    when(geoJsonConversionJobRepositoryMock.findById(any()))
        .thenReturn(
            Optional.of(
                GeoJsonConversionJob.builder()
                    .id(CONVERSION_JOB_ID)
                    .zoneDetectionJobType(MACHINE)
                    .zoneDetectionJobId(ZONE_DETECTION_JOB_ID)
                    .build()));
    when(zoneDetectionJobServiceMock.findById(ZONE_DETECTION_JOB_ID))
        .thenReturn(
            ZoneDetectionJob.builder().id(ZONE_DETECTION_JOB_ID).zoneName("dummyZoneName").build());
    when(machineDetectedTileRepositoryMock.findAllByZdjJobId(any(), any()))
        .thenReturn(List.of(machineDetectedTile()));
    when(geoJsonConverterMock.convert(any())).thenReturn(new GeoJson(List.of()));
    when(fileWriterMock.writeAsByte(any())).thenReturn(null);
    when(fileWriterMock.write(any(), any(), any())).thenReturn(mock(File.class));
    when(bucketComponentMock.upload(any(), any())).thenReturn(mock(FileHash.class));

    assertDoesNotThrow(
        () ->
            subject.accept(
                GeoJsonConversionTask.builder().page(1).jobId(CONVERSION_JOB_ID).build()));
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(humanDetectedTileRepositoryMock, never()).findAllByJobId(any(), any());
    verify(machineDetectedTileRepositoryMock, only()).findAllByZdjJobId(any(), any());
    verify(geoJsonConverterMock, only()).convert(listCaptor.capture());
    var detectedTile = (DetectedTile) listCaptor.getValue().getFirst();
    assertEquals(expectedDetectedTile(), detectedTile);
  }

  @Test
  void consumes_ko() {
    var conversionJobId = "conversionJobId";
    when(geoJsonConversionJobRepositoryMock.findById(conversionJobId)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> subject.accept(GeoJsonConversionTask.builder().jobId(conversionJobId).build()));
  }

  private static DetectedTile expectedDetectedTile() {
    return DetectedTile.builder()
        .tile(humanDetectedTile().getTile())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedTileId("detectedTileId")
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(POLYGON)
                                    .actualInstanceStringValue("{}")
                                    .build())
                            .build())
                    .build()))
        .build();
  }

  private static HumanDetectedTile humanDetectedTile() {
    return HumanDetectedTile.builder()
        .tile(new Tile())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedTileId("detectedTileId")
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(POLYGON)
                                    .actualInstanceStringValue("{}")
                                    .build())
                            .build())
                    .build()))
        .build();
  }

  private static MachineDetectedTile machineDetectedTile() {
    return MachineDetectedTile.builder()
        .tile(new Tile())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .detectedTileId("detectedTileId")
                    .computedConfidence(0.9)
                    .feature(
                        Feature.builder()
                            .id("feature_id")
                            .zoom(20)
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(POLYGON)
                                    .actualInstanceStringValue("{}")
                                    .build())
                            .build())
                    .build()))
        .build();
  }
}
