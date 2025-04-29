package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionAssemblySucceededService
    implements Consumer<GeoJsonConversionAssemblySucceeded> {
  private final DetectionFinishedMailer mailer;

  @SneakyThrows
  @Override
  public void accept(GeoJsonConversionAssemblySucceeded event) {
    var geoJsonConversionJob = event.getGeoJsonConversionJob();
    var succeededDatetime = geoJsonConversionJob.getStatus().getCreationDatetime();
    var zoneName = geoJsonConversionJob.getZoneName();
    var formattedCreationDatetime =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .format(succeededDatetime.atZone(ZoneId.of("Europe/Paris")));

    var emailSubject =
        String.format("Analyse sur la zone %s terminée le %s", zoneName, formattedCreationDatetime);
    mailer.accept(geoJsonConversionJob.getEmailReceiver(), emailSubject);
  }
}
