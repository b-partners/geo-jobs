package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneDetectionJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ZoneDetectionJobFailedService implements Consumer<ZoneDetectionJobFailed> {
  private final DetectionFinishedMailer mailer;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(ZoneDetectionJobFailed event) {
    var jobId = event.getFailedJobId();
    var zoneDetectionJob = zoneDetectionJobService.findById(jobId);
    var optionalDetection = detectionRepository.findByZdjId(zoneDetectionJob.getId());
    StringBuilder subjectBuilder = new StringBuilder();
    if (optionalDetection.isPresent()) {
      subjectBuilder
          .append("Erreur lors du traitement de la détection portant l'ID ")
          .append(optionalDetection.get().getEndToEndId());
    } else {
      subjectBuilder
          .append("Erreur lors du traitement de la détection machine (ZDJ=")
          .append(zoneDetectionJob.getId())
          .append(")");
    }
    mailer.accept(zoneDetectionJob.getEmailReceiver(), subjectBuilder.toString());
  }
}
