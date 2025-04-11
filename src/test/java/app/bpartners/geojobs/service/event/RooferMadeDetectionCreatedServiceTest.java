package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.RooferMadeDetectionCreated;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class RooferMadeDetectionCreatedServiceTest {
  private static final String ZDJ_ID = "zdj_id";
  private static final String DETECTION_ID = "detection_id";
  ZoneDetectionJobService zoneDetectionJobService = mock();
  DetectionRepository detectionRepository = mock();
  TileObjectDetector detector = mock();
  MachineDetectedTileRepository machineDetectedTileRepository = mock();
  GeoJsonConverter geoJsonConverter = mock();
  BucketComponent bucketComponent = mock();
  DetectionMapper detectionMapper = mock();
  FileWriter fileWriter = mock();
  DetectionMaskCreator detectionMaskCreator = mock();
  EventProducer eventProducer = mock();
  RooferMadeDetectionCreatedService subject;

  @BeforeEach
  void setUp() {
    subject =
        new RooferMadeDetectionCreatedService(
            zoneDetectionJobService,
            detectionRepository,
            detector,
            machineDetectedTileRepository,
            geoJsonConverter,
            bucketComponent,
            detectionMapper,
            fileWriter,
            detectionMaskCreator,
            eventProducer);
    when(detectionRepository.findById(DETECTION_ID))
        .thenReturn(Optional.ofNullable(Detection.builder().build()));
    when(detectionMaskCreator.apply(any())).thenReturn(Map.of());
    when(zoneDetectionJobService.getMachineZdjFromZdjId(any())).thenReturn(machineDetectionJob());
    when(zoneDetectionJobService.getTasks(any())).thenReturn(List.of());
    when(machineDetectedTileRepository.saveAll(any())).thenReturn(List.of());
    when(geoJsonConverter.convert(any())).thenReturn(new GeoJson(List.of()));
    when(fileWriter.write(any(), any(), any())).thenReturn(mock(File.class));
  }

  @Test
  void accept_ok() {
    var detectionIdCaptor = ArgumentCaptor.forClass(String.class);
    var zdjIdCaptor = ArgumentCaptor.forClass(String.class);
    var zdjCaptor = ArgumentCaptor.forClass(ZoneDetectionJob.class);
    var event =
        RooferMadeDetectionCreated.builder().zdjId(ZDJ_ID).detectionId(DETECTION_ID).build();

    subject.accept(event);

    verify(detectionRepository, times(1)).findById(detectionIdCaptor.capture());
    verify(detectionRepository, times(1)).save(any());
    verify(detectionMaskCreator, only()).apply(any());
    verify(zoneDetectionJobService, times(1)).getMachineZdjFromZdjId(zdjIdCaptor.capture());
    verify(zoneDetectionJobService, times(1)).getTasks(zdjCaptor.capture());
    verify(geoJsonConverter, only()).convert(any());
    verify(eventProducer, only()).accept(any());
    verify(fileWriter, only()).write(any(), any(), any());
    verify(bucketComponent, only()).upload(any(), any());

    assertEquals(DETECTION_ID, detectionIdCaptor.getValue());
    assertEquals(ZDJ_ID, zdjIdCaptor.getValue());
    assertEquals(machineDetectionJob(), zdjCaptor.getValue());
  }

  private ZoneDetectionJob machineDetectionJob() {
    return ZoneDetectionJob.builder().id(ZDJ_ID).build();
  }
}
