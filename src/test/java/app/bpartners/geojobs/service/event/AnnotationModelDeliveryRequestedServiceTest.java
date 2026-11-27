package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.HumanDetectionJobCreated;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationFalsePositiveDeliveryRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationModelDeliveryRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationTruePositiveDeliveryRequested;
import app.bpartners.geojobs.endpoint.event.model.annotation.AnnotationWithoutDetectedObjectDeliveryRequested;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.HumanDetectionJob;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.HumanDetectionJobService;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnnotationModelDeliveryRequestedServiceTest {
  private static final String ZDJ_ID = "zdjId";
  private static final String HUMAN_ZDJ_ID = "humanZdjId";
  private static final String ANNOTATION_JOB_ID = "annotationJobId";
  private static final double MIN_CONFIDENCE = 0.8;

  ZoneDetectionJobService zoneDetectionJobServiceMock = mock();
  MachineDetectedTileRepository machineDetectedTileRepositoryMock = mock();
  HumanDetectionJobService humanDetectionJobServiceMock = mock();
  EventProducer eventProducerMock = mock();
  AnnotationModelDeliveryRequestedService<AnnotationModelDeliveryRequested> subject =
      new AnnotationModelDeliveryRequestedService<>(
          zoneDetectionJobServiceMock,
          machineDetectedTileRepositoryMock,
          humanDetectionJobServiceMock,
          eventProducerMock);

  private final List<DetectableObjectConfiguration> configurations =
      List.of(DetectableObjectConfiguration.builder().objectType(PISCINE).build());
  private final List<MachineDetectedTile> tiles =
      List.of(MachineDetectedTile.builder().id("tile1").build());

  private void givenHumanJobCreatedFor(List<MachineDetectedTile> machineDetectedTiles) {
    when(zoneDetectionJobServiceMock.getHumanZdjFromZdjId(ZDJ_ID))
        .thenReturn(ZoneDetectionJob.builder().id(HUMAN_ZDJ_ID).zoneName("zoneName").build());
    when(humanDetectionJobServiceMock.create(
            eq(ANNOTATION_JOB_ID),
            anyString(),
            eq(machineDetectedTiles),
            eq(HUMAN_ZDJ_ID),
            eq(configurations)))
        .thenReturn(
            HumanDetectionJob.builder()
                .id("humanJobId")
                .detectableObjectConfigurations(configurations)
                .build());
  }

  private String publishedJobName() {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(captor.capture());
    var event = (HumanDetectionJobCreated) captor.getValue().getFirst();
    assertEquals("humanJobId", event.getHumanDetectionJobId());
    return event.getJobName();
  }

  @Test
  void accept_true_positive_queries_tiles_above_confidence_and_publishes_named_job() {
    when(machineDetectedTileRepositoryMock.findAllInDoubtByZdjJobId(ZDJ_ID, MIN_CONFIDENCE, true))
        .thenReturn(tiles);
    givenHumanJobCreatedFor(tiles);

    subject.accept(
        new AnnotationTruePositiveDeliveryRequested(
            ZDJ_ID, ANNOTATION_JOB_ID, MIN_CONFIDENCE, configurations));

    var jobName = publishedJobName();
    assertTrue(
        jobName.startsWith(
            "zoneName - [PISCINE] 1 images with object detection confidence >= 80.0% "));
  }

  @Test
  void accept_false_positive_queries_tiles_below_confidence_and_publishes_named_job() {
    when(machineDetectedTileRepositoryMock.findAllInDoubtByZdjJobId(ZDJ_ID, MIN_CONFIDENCE, false))
        .thenReturn(tiles);
    givenHumanJobCreatedFor(tiles);

    subject.accept(
        new AnnotationFalsePositiveDeliveryRequested(
            ZDJ_ID, ANNOTATION_JOB_ID, MIN_CONFIDENCE, configurations));

    var jobName = publishedJobName();
    assertTrue(
        jobName.startsWith(
            "zoneName - [PISCINE] 1 images with object detection confidence < 80.0% "));
  }

  @Test
  void accept_without_detected_object_queries_tiles_without_object_and_publishes_named_job() {
    when(machineDetectedTileRepositoryMock.findAllInDoubtTilesWithoutObjectByZdjJobId(ZDJ_ID))
        .thenReturn(tiles);
    givenHumanJobCreatedFor(tiles);

    subject.accept(
        new AnnotationWithoutDetectedObjectDeliveryRequested(
            ZDJ_ID, ANNOTATION_JOB_ID, MIN_CONFIDENCE, configurations));

    var jobName = publishedJobName();
    assertTrue(jobName.startsWith("zoneName - [PISCINE] 1 images without object 80.0 "));
  }

  @Test
  void accept_without_tiles_creates_human_job_but_publishes_nothing() {
    when(machineDetectedTileRepositoryMock.findAllInDoubtByZdjJobId(ZDJ_ID, MIN_CONFIDENCE, true))
        .thenReturn(List.of());
    givenHumanJobCreatedFor(List.of());

    subject.accept(
        new AnnotationTruePositiveDeliveryRequested(
            ZDJ_ID, ANNOTATION_JOB_ID, MIN_CONFIDENCE, configurations));

    verify(humanDetectionJobServiceMock)
        .create(
            eq(ANNOTATION_JOB_ID),
            anyString(),
            eq(List.of()),
            eq(HUMAN_ZDJ_ID),
            eq(configurations));
    verifyNoInteractions(eventProducerMock);
  }
}
