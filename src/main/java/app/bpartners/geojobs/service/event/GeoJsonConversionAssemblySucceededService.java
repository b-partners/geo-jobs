package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.service.DetectionFinishedMailer;
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
    mailer.accept(
        geoJsonConversionJob.getEmailReceiver(),
        geoJsonConversionJob.getZoneName(),
        geoJsonConversionJob.getStatus().getCreationDatetime());
  }
}
