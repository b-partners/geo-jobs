package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.AnnotationDeliveryConfigurationRepository;
import app.bpartners.geojobs.repository.model.annotation.AnnotationDeliveryConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionJobSucceededServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  private static final double MIN_CONFIDENCE_FOR_DELIVERY = 0.95;
  AnnotationDeliveryConfigurationRepository configurationRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(configurationRepositoryMock, eventProducerMock);

  @Test
  void succeeded_and_triggers_annotation_delivery_job_requested() {
    var annotationDeliveryConfigurationMock = mock(AnnotationDeliveryConfiguration.class);
    when(annotationDeliveryConfigurationMock.getMinimumConfidenceForDelivery())
        .thenReturn(MIN_CONFIDENCE_FOR_DELIVERY);
    when(configurationRepositoryMock.findLatestConfiguration())
        .thenReturn(Optional.of(annotationDeliveryConfigurationMock));

    subject.accept(ZoneDetectionJobSucceeded.builder().succeededJobId(MOCK_JOB_ID).build());

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(1)).accept(listCaptor.capture());
    var annotationJobDeliveryRequested =
        ((AnnotationDeliveryJobRequested) listCaptor.getValue().getFirst());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithObjectsIdTruePositive());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithObjectsIdFalsePositive());
    assertNotNull(annotationJobDeliveryRequested.getAnnotationJobWithoutObjectsId());
    assertEquals(MOCK_JOB_ID, annotationJobDeliveryRequested.getJobId());
  }

  @Test
  void fails_to_find_annotation_delivery_configuration() {
    when(configurationRepositoryMock.findLatestConfiguration()).thenReturn(Optional.empty());

    var actual =
        assertThrows(
            IllegalStateException.class,
            () ->
                subject.accept(
                    ZoneDetectionJobSucceeded.builder().succeededJobId(MOCK_JOB_ID).build()));

    assertEquals("No annotation delivery configuration found", actual.getMessage());
  }
}
