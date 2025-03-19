package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import jakarta.mail.internet.InternetAddress;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionAssemblySucceededService
    implements Consumer<GeoJsonConversionAssemblySucceeded> {
  private final Mailer mailer;

  @SneakyThrows
  @Override
  public void accept(GeoJsonConversionAssemblySucceeded event) {
    var geoJsonConversionJob = event.getGeoJsonConversionJob();
    var emailReceiver = geoJsonConversionJob.getEmailReceiver();
    var zoneName = geoJsonConversionJob.getZoneName();
    var creationDatetime = geoJsonConversionJob.getStatus().getCreationDatetime();
    var formattedCreationDatetime =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss").format(creationDatetime);

    mailer.accept(
        new Email(
            new InternetAddress(emailReceiver),
            List.of(new InternetAddress("tech@bpartners.app")),
            List.of(),
            String.format(
                "Analyse sur l'adresse %s terminée le %s", zoneName, formattedCreationDatetime),
            null,
            List.of()));
  }
}
