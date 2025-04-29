package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.MOISISSURE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.job.model.JobStatus;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionJobSucceededServiceTest {
  private static final double MIN_CONFIDENCE_FOR_DELIVERY = 0.95;
  AnnotationDeliveryConfigurationRepository configurationRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  DetectableObjectConfigurationRepository detectableObjectConfigurationRepositoryMock = mock();
  DetectionFinishedMailer detectionFinishedMailerMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(
          configurationRepositoryMock,
          zoneDetectionJobServiceMock,
          geoJsonConversionJobServiceMock,
          eventProducerMock,
          machineDetectedTileRepositoryMock,
          detectableObjectConfigurationRepositoryMock,
          detectionFinishedMailerMock);

  @BeforeEach
  void setUp() {
    // so that detection always has detected tile - must be overridden for specific test
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(any(), any()))
        .thenReturn(1L);
    when(detectableObjectConfigurationRepositoryMock.findAllByDetectionJobId(any()))
        .thenReturn(someObjectConfigurations());
  }

  private @NotNull List<DetectableObjectConfiguration> someObjectConfigurations() {
    return List.of(
        DetectableObjectConfiguration.builder().objectType(USURE).build(),
        DetectableObjectConfiguration.builder().objectType(MOISISSURE).build());
  }

  @Test
  void trigger_detection_finished_mailer_when_no_detect_tile_found() {
    var succeededJobId = randomUUID().toString();
    var succeededZoneDetectionJobMock = mock(ZoneDetectionJob.class);
    var jobStatus = mock(JobStatus.class);
    reset(machineDetectedTileRepositoryMock);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            succeededJobId, USURE.name()))
        .thenReturn(0L);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            succeededJobId, MOISISSURE.name()))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(succeededJobId))
        .thenReturn(1L);
    var emailReceiver = "email@email.com";
    var zoneName = "My address";
    var creationDatetime = Instant.parse("2025-03-01T03:00:00Z");
    var emailSubject =
        String.format(
            "Analyse sur la zone %s terminée le %s",
            zoneName,
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .format(creationDatetime.atZone(ZoneId.of("Europe/Paris"))));
    when(succeededZoneDetectionJobMock.getEmailReceiver()).thenReturn(emailReceiver);
    when(succeededZoneDetectionJobMock.getZoneName()).thenReturn(zoneName);
    when(jobStatus.getCreationDatetime()).thenReturn(creationDatetime);
    when(succeededZoneDetectionJobMock.getStatus()).thenReturn(jobStatus);
    when(zoneDetectionJobServiceMock.findById(succeededJobId))
        .thenReturn(succeededZoneDetectionJobMock);

    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobSucceeded(succeededJobId)));

    verify(detectionFinishedMailerMock, only()).accept(emailReceiver, emailSubject);
    verify(configurationRepositoryMock, never()).findLatestConfiguration();
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void succeeded_and_triggers_annotation_delivery_job_requested() {
    var annotationDeliveryConfigurationMock = mock(AnnotationDeliveryConfiguration.class);
    var succeededJobId = randomUUID().toString();
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(succeededJobId))
        .thenReturn(1L);
    when(annotationDeliveryConfigurationMock.getMinimumConfidenceForDelivery())
        .thenReturn(MIN_CONFIDENCE_FOR_DELIVERY);
    when(configurationRepositoryMock.findLatestConfiguration())
        .thenReturn(Optional.of(annotationDeliveryConfigurationMock));

    subject.accept(ZoneDetectionJobSucceeded.builder().succeededJobId(succeededJobId).build());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var annotationJobDeliveryRequested =
        ((AnnotationDeliveryJobRequested) listCaptor.getValue().getFirst());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithObjectsIdTruePositive());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithObjectsIdFalsePositive());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithoutObjectsId());
    assertEquals(succeededJobId, annotationJobDeliveryRequested.getJobId());
  }

  @Test
  void fails_to_find_annotation_delivery_configuration() {
    var succeededJobId = randomUUID().toString();
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(succeededJobId))
        .thenReturn(1L);
    when(configurationRepositoryMock.findLatestConfiguration()).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            IllegalStateException.class,
            () ->
                subject.accept(
                    ZoneDetectionJobSucceeded.builder().succeededJobId(succeededJobId).build()));

    assertEquals("No annotation delivery configuration found", actual.getMessage());
  }

  @Test
  void process_geo_json_conversion_job_when_any_in_doubt_detected_tile() {
    var succeededJobId = randomUUID().toString();
    var zoneDetectionJobMock = mock(ZoneDetectionJob.class);
    when(zoneDetectionJobServiceMock.countInDoubtDetectedTileToDeliveryById(succeededJobId))
        .thenReturn(0L);
    when(zoneDetectionJobServiceMock.findById(succeededJobId)).thenReturn(zoneDetectionJobMock);

    assertDoesNotThrow(
        () ->
            subject.accept(
                ZoneDetectionJobSucceeded.builder().succeededJobId(succeededJobId).build()));

    verify(geoJsonConversionJobServiceMock, times(1))
        .getOrComputeGeoJsonConversionJob(zoneDetectionJobMock);
    verify(configurationRepositoryMock, never()).findLatestConfiguration();
    verify(eventProducerMock, never()).accept(any());
  }
}
