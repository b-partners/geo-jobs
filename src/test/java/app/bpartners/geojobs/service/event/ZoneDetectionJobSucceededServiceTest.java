package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ZoneDetectionJobSucceededServiceTest {
  private static final String MOCK_JOB_ID = "mock_job_id";
  EventProducer eventProducerMock = mock();
  ZoneDetectionJobSucceededService subject =
      new ZoneDetectionJobSucceededService(eventProducerMock);

  @Test
  void accept_ok() {
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
}
