package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZoneTilingJobFailedService implements Consumer<ZoneTilingJobFailed> {
  private final DetectionFinishedMailer mailer;
  private final DetectionRepository detectionRepository;

  @Override
  public void accept(ZoneTilingJobFailed event) {
    var job = event.getFailedJob();

    var optionalDetection = detectionRepository.findByZtjId(job.getId());
    StringBuilder subjectBuilder = new StringBuilder();
    if (optionalDetection.isPresent()) {
      subjectBuilder
          .append("Erreur survenue lors du traitement de la détection portant l'ID ")
          .append(optionalDetection.get().getEndToEndId());
    } else {
      subjectBuilder
          .append("Erreur survenue lors du traitement du pavage (ZDJ=")
          .append(job.getId())
          .append(")");
    }
    mailer.accept(job.getEmailReceiver(), subjectBuilder.toString());
  }
}
