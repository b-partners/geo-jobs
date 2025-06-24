package app.bpartners.geojobs.service;

import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetectionService {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;

  public Detection getByZoneDetectionJob(ZoneDetectionJob zoneDetectionJob) {
    ZoneDetectionJob machineZDJ =
        zoneDetectionJobService.getMachineZdjFromZdjId(zoneDetectionJob.getId());
    ZoneDetectionJob humanZDJ;
    try {
      humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJob.getId());
    } catch (IllegalArgumentException ignored) {
      humanZDJ = null;
    }
    return detectionRepository
        .findByZdjId(humanZDJ == null ? null : humanZDJ.getId())
        .orElseGet(
            () -> {
              var optionalDetectionFromMachineZDJ =
                  detectionRepository.findByZdjId(machineZDJ.getId());
              if (optionalDetectionFromMachineZDJ.isPresent()) {
                return optionalDetectionFromMachineZDJ.orElseThrow();
              }
              return null;
            });
  }
}
