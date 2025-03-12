package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.job.model.Status.HealthStatus.UNKNOWN;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.PENDING;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.USURE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionTaskCreated;
import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.HumanDetectedTileRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GeoJsonConversionJobCreatedServiceTest {
  HumanDetectedTileRepository humanDetectedTileRepositoryMock = mock();
  GeoJsonConversionTaskRepository geoJsonConversionTaskRepositoryMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  EventProducer eventProducerMock = mock();
  DetectableObjectConfigurationRepository objectConfigurationRepositoryMock = mock();
  GeoJsonConversionJobCreatedService subject =
      new GeoJsonConversionJobCreatedService(
          humanDetectedTileRepositoryMock,
          geoJsonConversionTaskRepositoryMock,
          machineDetectedTileRepositoryMock,
          eventProducerMock,
          objectConfigurationRepositoryMock);

  @Test
  void
      compute_geo_json_conversion_task_created_with_machine_detected_tile_filtered_by_detectable_type() {
    var zoneDetectionJobId = randomUUID().toString();
    var geoJsonConversionJobId = randomUUID().toString();
    var zoneDetectionJobType = MACHINE;
    var geoJsonConversionJobMock = mock(GeoJsonConversionJob.class);

    when(geoJsonConversionJobMock.getId()).thenReturn(geoJsonConversionJobId);
    when(geoJsonConversionJobMock.getZoneDetectionJobId()).thenReturn(zoneDetectionJobId);
    when(geoJsonConversionJobMock.getZoneDetectionJobType()).thenReturn(zoneDetectionJobType);
    when(objectConfigurationRepositoryMock.findAllByDetectionJobId(zoneDetectionJobId))
        .thenReturn(
            List.of(
                DetectableObjectConfiguration.builder()
                    .objectType(DetectableType.TOITURE_REVETEMENT)
                    .build(),
                DetectableObjectConfiguration.builder().objectType(HUMIDITE).build(),
                DetectableObjectConfiguration.builder().objectType(USURE).build()));
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            zoneDetectionJobId, DetectableType.TOITURE_REVETEMENT))
        .thenReturn(0L);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            zoneDetectionJobId, HUMIDITE))
        .thenReturn(100L);
    when(machineDetectedTileRepositoryMock.countByZdjJobIdAndDetectableType(
            zoneDetectionJobId, USURE))
        .thenReturn(600L);
    when(geoJsonConversionTaskRepositoryMock.saveAll(any()))
        .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

    assertDoesNotThrow(
        () -> subject.accept(new GeoJsonConversionJobCreated(geoJsonConversionJobMock)));

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times(4)).accept(eventCaptor.capture());
    var geoJsonConversionJobCreatedEvents =
        eventCaptor.getAllValues().stream()
            .map(List::getFirst)
            .map(retrieveObjectType(GeoJsonConversionTaskCreated.class))
            .filter(Objects::nonNull)
            .peek(computeGeoJsonConversionTaskCreatedNullValuesForRandomAttribution())
            .toList();
    var geoJsonConversionJobStatusRecomputingSubmittedEvent =
        eventCaptor.getAllValues().stream()
            .map(List::getFirst)
            .map(retrieveObjectType(GeoJsonConversionJobStatusRecomputingSubmitted.class))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow();
    assertTrue(
        geoJsonConversionJobCreatedEvents.contains(
            geoJsonConversionTaskCreatedBuilder(geoJsonConversionJobId, HUMIDITE, 1)));
    assertTrue(
        geoJsonConversionJobCreatedEvents.contains(
            geoJsonConversionTaskCreatedBuilder(geoJsonConversionJobId, USURE, 1)));
    assertTrue(
        geoJsonConversionJobCreatedEvents.contains(
            geoJsonConversionTaskCreatedBuilder(geoJsonConversionJobId, USURE, 2)));
    assertEquals(
        new GeoJsonConversionJobStatusRecomputingSubmitted(geoJsonConversionJobId),
        geoJsonConversionJobStatusRecomputingSubmittedEvent);
  }

  private GeoJsonConversionTaskCreated geoJsonConversionTaskCreatedBuilder(
      String geoJsonConversionJobId, DetectableType humidite, int pageNumber) {
    return GeoJsonConversionTaskCreated.builder()
        .geoJsonConversionTask(
            someGeoJsonConversionTask(geoJsonConversionJobId, humidite, pageNumber))
        .build();
  }

  private static <T> @NotNull Function<Object, T> retrieveObjectType(Class<T> clazz) {
    return object -> {
      if (clazz.isInstance(object)) {
        return clazz.cast(object);
      }
      return null;
    };
  }

  private static @NotNull Consumer<GeoJsonConversionTaskCreated>
      computeGeoJsonConversionTaskCreatedNullValuesForRandomAttribution() {
    return geoJsonConversionJobCreatedEvent -> {
      geoJsonConversionJobCreatedEvent.getGeoJsonConversionTask().setId(null);
      geoJsonConversionJobCreatedEvent.getGeoJsonConversionTask().setSubmissionInstant(null);
      geoJsonConversionJobCreatedEvent
          .getGeoJsonConversionTask()
          .getStatusHistory()
          .forEach(status -> status.setCreationDatetime(null));
    };
  }

  private GeoJsonConversionTask someGeoJsonConversionTask(
      String geoJsonConversionJobId, DetectableType detectableType, int pageNumber) {
    var geoJsonConversionTask =
        GeoJsonConversionTask.builder()
            .id(null)
            .jobId(geoJsonConversionJobId)
            .page(pageNumber)
            .detectableType(detectableType)
            .submissionInstant(null)
            .build();
    geoJsonConversionTask.hasNewStatus(
        Status.builder()
            .progression(PENDING)
            .health(UNKNOWN)
            .creationDatetime(null)
            .message(null)
            .build());
    return geoJsonConversionTask;
  }
}
