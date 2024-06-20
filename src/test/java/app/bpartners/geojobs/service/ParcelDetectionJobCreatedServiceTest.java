package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PATHWAY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ParcelDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.TileDetectionTaskCreated;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.TileDetectionTaskRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionJob;
import app.bpartners.geojobs.service.event.ParcelDetectionJobCreatedService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ParcelDetectionJobCreatedServiceTest {
  private static final String JOB_ID = "jobId";
  TileDetectionTaskRepository taskRepositoryMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  ParcelDetectionJobCreatedService subject =
      new ParcelDetectionJobCreatedService(
          taskRepositoryMock, objectConfigurationRepositoryMock, eventProducerMock);

  @Test
  void accept_ok() {
    var parcelDetectionJob = ParcelDetectionJob.builder().id(JOB_ID).build();
    var zoneDetectionJobId = "zoneDetectionJobId";
    when(taskRepositoryMock.findAllByJobId(JOB_ID))
        .thenReturn(List.of(aTileDetectionTask("task1"), aTileDetectionTask("task2")));
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(zoneDetectionJobId))
        .thenReturn(
            List.of(
                DetectableObjectConfiguration.builder()
                    .id("objectConfiguration1")
                    .objectType(PATHWAY)
                    .build()));

    assertDoesNotThrow(
        () ->
            subject.accept(new ParcelDetectionJobCreated(zoneDetectionJobId, parcelDetectionJob)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(taskRepositoryMock, times(1)).findAllByJobId(JOB_ID);
    verify(objectConfigurationRepositoryMock, times(1)).findAllByDetectionJobId(zoneDetectionJobId);
    verify(eventProducerMock, times(2)).accept(eventCaptor.capture());
    var events = eventCaptor.getAllValues();
    var event1 = (List<TileDetectionTaskCreated>) events.getFirst();
    var event2 = (List<TileDetectionTaskCreated>) events.getLast();

    assertTrue(
        event1.contains(
            new TileDetectionTaskCreated(aTileDetectionTask("task1"), List.of(PATHWAY))));
    assertTrue(
        event2.contains(
            new TileDetectionTaskCreated(aTileDetectionTask("task2"), List.of(PATHWAY))));
  }

  private TileDetectionTask aTileDetectionTask(String id) {
    return TileDetectionTask.builder().id(id).build();
  }
}
