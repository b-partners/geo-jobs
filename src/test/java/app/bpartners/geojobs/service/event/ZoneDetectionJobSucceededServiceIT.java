package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.DetectionTaskServiceIT.detectedTile;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.*;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.detection.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  public static final String SUCCEEDED_JOB_ID = "succeededJobId";
  public static final String HUMAN_ZDJ_ID = "humanZdjId";
  @MockBean EventProducer eventProducer;
  @Autowired ZoneDetectionJobSucceededService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;
  @Autowired private DetectedTileRepository detectedTileRepository;
  @Autowired private DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired private HumanDetectionJobRepository humanDetectionJobRepository;
  @Autowired private DetectionTaskRepository detectionTaskRepository;

  @BeforeEach
  void setUp() {
    jobRepository.save(
        ZoneDetectionJob.builder()
            .id(SUCCEEDED_JOB_ID)
            .detectionType(ZoneDetectionJob.DetectionType.MACHINE)
            .build());
    jobRepository.save(
        ZoneDetectionJob.builder()
            .id(HUMAN_ZDJ_ID)
            .detectionType(ZoneDetectionJob.DetectionType.HUMAN)
            .build());
    detectionTaskRepository.save(
        DetectionTask.builder()
            .id("detectionTaskId")
            .parcels(
                List.of(
                    Parcel.builder().id("parcel1Id").build(),
                    Parcel.builder().id("parcel2Id").build()))
            .build());
    detectedTileRepository.saveAll(
        List.of(
            detectedTile(SUCCEEDED_JOB_ID, "tile1Id", "parcel1Id", "detectedObjectId1", 0.8),
            detectedTile(SUCCEEDED_JOB_ID, "tile2Id", "parcel2Id", "detectedObjectId2", 0.5)));
    objectConfigurationRepository.save(
        DetectableObjectConfiguration.builder()
            .id("detectableObjectConfigurationId")
            .confidence(0.7)
            .objectType(DetectableType.ROOF)
            .detectionJobId(SUCCEEDED_JOB_ID)
            .build());
  }

  @Test
  void handle_human_detection_job() {
    List<HumanDetectionJob> before = humanDetectionJobRepository.findAll();

    subject.accept(new ZoneDetectionJobSucceeded(SUCCEEDED_JOB_ID, HUMAN_ZDJ_ID));

    List<HumanDetectionJob> after = humanDetectionJobRepository.findAll();
    assertTrue(before.isEmpty());
    assertEquals(1, after.size());
    assertEquals(HUMAN_ZDJ_ID, after.get(0).getZoneDetectionJobId());
    assertFalse(after.get(0).getInDoubtTiles().isEmpty());
  }
}
