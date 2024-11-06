package app.bpartners.geojobs.service.event;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionJobStatusChanged;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionJob;
import app.bpartners.geojobs.service.StatusChangedHandler;
import app.bpartners.geojobs.service.StatusHandler;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeoJsonConversionJobStatusChangedService
    implements Consumer<GeoJsonConversionJobStatusChanged> {
  private final StatusChangedHandler statusChangedHandler;
  private final EventProducer eventProducer;

  @Override
  public void accept(GeoJsonConversionJobStatusChanged event) {
    var oldJob = event.getOldJob();
    var newJob = event.getNewJob();

    var onFinishedHandler = new OnFinishedHandler(newJob, eventProducer);

    statusChangedHandler.handle(
        event, newJob.getStatus(), oldJob.getStatus(), onFinishedHandler, onFinishedHandler);
  }

  private record OnFinishedHandler(GeoJsonConversionJob newJob, EventProducer eventProducer)
      implements StatusHandler {

    @Override
    public String performAction() {
      eventProducer.accept(
          List.of(
              GeoJsonConversionAssemblyInitiated.builder()
                  .geoJsonConversionJobId(newJob.getId())
                  .build()));
      return "GeoJsonConversionJob(id=" + newJob.getId() + ") finished, assembly initiated";
    }
  }
}
