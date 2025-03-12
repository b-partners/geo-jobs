package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionJobSucceededServiceTest {
  private static final double MIN_CONFIDENCE_FOR_DELIVERY = 0.95;
  AnnotationDeliveryConfigurationRepository configurationRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  GeoJsonConversionJobService geoJsonConversionJobServiceMock = mock();
  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(
          configurationRepositoryMock,
          zoneDetectionJobServiceMock,
          geoJsonConversionJobServiceMock,
          eventProducerMock);

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
