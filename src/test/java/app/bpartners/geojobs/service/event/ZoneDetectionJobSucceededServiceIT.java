package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.service.DetectionTaskServiceIT.detectedTile;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.gen.ZoneDetectionJobSucceeded;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectedTileRepository;
import app.bpartners.geojobs.repository.HumanDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.detection.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("TODO: enable when 21 java source is fixed")
class ZoneDetectionJobSucceededServiceIT extends FacadeIT {
  public static final String SUCCEEDED_JOB_ID = "succeededJobId";
  public static final String HUMAN_ZDJ_ID = "humanZdjId";
  @Autowired ZoneDetectionJobSucceededService subject;
  @Autowired private ZoneDetectionJobRepository jobRepository;
  @Autowired private DetectedTileRepository detectedTileRepository;
  @Autowired private DetectableObjectConfigurationRepository objectConfigurationRepository;
  @Autowired private HumanDetectionJobRepository humanDetectionJobRepository;

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

    detectedTileRepository.saveAll(
        List.of(detectedTile(SUCCEEDED_JOB_ID, 0.8), detectedTile(SUCCEEDED_JOB_ID, 0.5)));
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
