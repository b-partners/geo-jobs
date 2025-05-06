package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionAssemblySucceededService
    implements Consumer<GeoJsonConversionAssemblySucceeded> {
  private static final String DETECTION_SUCCEEDED_MAIL_TEMPLATE = "detection_succeeded_template";
  private final DetectionFinishedMailer mailer;
  private final HTMLTemplateParser htmlTemplateParser;
  private final DetectionRepository detectionRepository;
  private final BucketComponent bucketComponent;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;

  @SneakyThrows
  @Override
  public void accept(GeoJsonConversionAssemblySucceeded event) {
    var geoJsonConversionJob = event.getGeoJsonConversionJob();
    var succeededDatetime = geoJsonConversionJob.getStatus().getCreationDatetime();
    var zoneName = geoJsonConversionJob.getZoneName();
    var formattedCreationDatetime =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(succeededDatetime.atZone(ZoneId.of("Europe/Paris")));

    var emailSubject =
        String.format("Analyse sur la zone %s terminée le %s", zoneName, formattedCreationDatetime);
    mailer.accept(
        geoJsonConversionJob.getEmailReceiver(), emailSubject, getEmailBody(geoJsonConversionJob));
  }

  private String getEmailBody(GeoJsonConversionJob geoJsonConversionJob) {
    var zoneDetectionJobId = geoJsonConversionJob.getZoneDetectionJobId();
    var optionalZoneDetectionJob = zoneDetectionJobRepository.findById(zoneDetectionJobId);
    var optionalDetection = detectionRepository.findByZdjId(zoneDetectionJobId);
    var geoJsonConversionJobStatus = geoJsonConversionJob.getStatus();

    Context context = new Context();
    context.setVariable("job", geoJsonConversionJob);
    context.setVariable("zoneDetectionJob", optionalZoneDetectionJob.orElse(null));
    context.setVariable("detection", optionalDetection.orElse(null));
    context.setVariable(
        "geojsonUrl",
        optionalDetection
            .map(detection -> bucketComponent.presign(detection.getGeojsonS3FileKey()))
            .orElse(null));
    context.setVariable(
        "geoJsonSucceededDatetime",
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .format(
                geoJsonConversionJobStatus
                    .getCreationDatetime()
                    .atZone(ZoneId.of("Europe/Paris"))));
    return htmlTemplateParser.apply(DETECTION_SUCCEEDED_MAIL_TEMPLATE, context);
  }
}
