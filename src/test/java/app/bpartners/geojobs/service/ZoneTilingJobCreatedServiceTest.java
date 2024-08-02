package app.bpartners.geojobs.service;

import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZTJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.ZoneTilingJobCreated;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.event.ZoneTilingJobCreatedService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZoneTilingJobCreatedServiceTest {
  ZoneTilingJobService zoneTilingJobService = mock();
  EventProducer eventProducer = mock();
  ZoneTilingJobCreatedService subject =
      new ZoneTilingJobCreatedService(zoneTilingJobService, eventProducer);

  @Test
  void should_fireTask_and_produce_event_if_fullDetection_is_not_null() {
    ZoneTilingJob ztj = ZoneTilingJob.builder().id("dummyId").build();
    CreateFullDetection createFullDetection = mock();
    ZoneTilingJobCreated zoneTilingJobCreated =
        ZoneTilingJobCreated.builder()
            .createFullDetection(createFullDetection)
            .zoneTilingJob(ztj)
            .build();

    subject.accept(zoneTilingJobCreated);

    verify(zoneTilingJobService, times(1)).fireTasks(ztj, createFullDetection);
    verify(eventProducer)
        .accept(List.of(new ZTJStatusRecomputingSubmitted(ztj.getId(), createFullDetection)));
  }
}
