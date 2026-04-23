package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobCreated;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneDetectionJobCreatedService implements Consumer<ZoneDetectionJobCreated> {
  private final ZoneDetectionJobService zoneDetectionJobService;

  @Override
  @Transactional
  public void accept(ZoneDetectionJobCreated zoneDetectionJobCreated) {
    long startTime = System.currentTimeMillis();
    try {
      var zoneDetectionJob = zoneDetectionJobCreated.getZoneDetectionJob();
      var detectionType = zoneDetectionJob.getDetectionType();
      switch (detectionType) {
        case MACHINE -> zoneDetectionJobService.fireTasks(zoneDetectionJob.getId());
        case HUMAN ->
            throw new NotImplementedException(
                "Processing ZDJ(type=HUMAN, id=" + zoneDetectionJob.getId() + " not supported yet");
        default -> throw new RuntimeException("Unknown ZDJ detection type : " + detectionType);
      }
    } finally {
      long elapsedTime = System.currentTimeMillis() - startTime;
      log.info(
          "{ \"operation\": \"ZoneDetectionJobCreated\", \"zoneDetectionJobId\": \"{}\","
              + " \"durationInMs\": \"{}\", \"isIntegrationTest\": \"{}\" }",
          zoneDetectionJobCreated.getZoneDetectionJob().getId(),
          elapsedTime,
          zoneDetectionJobCreated.getZoneDetectionJob().isIntegrationTest());
    }
  }
}
