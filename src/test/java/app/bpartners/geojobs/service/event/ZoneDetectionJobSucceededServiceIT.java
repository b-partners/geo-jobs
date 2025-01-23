package app.bpartners.geojobs.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationDeliveryJobRequested;
import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.detection.*;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  private final String succeededJobId;
  private final String zoneTilingJobId;
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobSucceededService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;

  ZoneDetectionJobSucceededServiceIT() {
    this.succeededJobId = randomUUID().toString();
    this.zoneTilingJobId = randomUUID().toString();
  }

  @BeforeEach
  void setUp() {
    jobRepository.saveAll(
        List.of(
            ZoneDetectionJob.builder()
                .id(succeededJobId)
                .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
                .zoneTilingJob(
                    ZoneTilingJob.builder()
                        .id(zoneTilingJobId)
                        .zoneName("dummy")
                        .emailReceiver("dummy")
                        .build())
                .build()));
  }

  @AfterEach
  void tearDown() {
    jobRepository.deleteById(succeededJobId);
  }

  @Test
  void zdj_succeeds_trigger_delivery_job() {
    assertDoesNotThrow(() -> subject.accept(new ZoneDetectionJobSucceeded(succeededJobId)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer, times(1)).accept(eventCaptor.capture());
    var capturedEvent = eventCaptor.getValue().getFirst();
    assertEquals(AnnotationDeliveryJobRequested.class, capturedEvent.getClass());
  }
}
