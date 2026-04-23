package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.zone.ZoneTilingJobFailed;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZoneTilingJobFailedService implements Consumer<ZoneTilingJobFailed> {
  private static final String ZONE_TILING_JOB_FAILED_TEMPLATE = "zone_tiling_job_failed_template";
  private final DetectionFinishedMailer mailer;
  private final DetectionRepository detectionRepository;
  private final HTMLTemplateParser htmlTemplateParser;

  @Override
  public void accept(ZoneTilingJobFailed event) {
    long startTime = System.currentTimeMillis();
    try {
      var failedJob = event.getFailedJob();

      var optionalDetection = detectionRepository.findByZtjId(failedJob.getId());
      StringBuilder subjectBuilder = new StringBuilder();
      if (optionalDetection.isPresent()) {
        subjectBuilder
            .append("Erreur survenue lors du traitement de la détection portant l'ID ")
            .append(optionalDetection.get().getEndToEndId());
      } else {
        subjectBuilder
            .append("Erreur survenue lors du traitement du pavage (ZTJ.id=")
            .append(failedJob.getId())
            .append(")");
      }
      mailer.accept(
          failedJob.getEmailReceiver(),
          subjectBuilder.toString(),
          getBody(optionalDetection, failedJob));
    } finally {
      long elapsedTime = System.currentTimeMillis() - startTime;
      log.info(
          "{ \"operation\": \"ZoneTilingJobFailed\", \"jobId\": \"{}\", \"durationInMs\":"
              + " \"{}\", \"isIntegrationTest\": \"{}\" }",
          event.getFailedJob().getId(),
          elapsedTime,
          event.getFailedJob().isIntegrationTest());
    }
  }

  private String getBody(Optional<Detection> optionalDetection, ZoneTilingJob failedJob) {
    Context context = new Context();
    context.setVariable("detection", optionalDetection.orElse(null));
    context.setVariable("failedJob", failedJob);
    return htmlTemplateParser.apply(ZONE_TILING_JOB_FAILED_TEMPLATE, context);
  }
}
