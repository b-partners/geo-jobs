package app.bpartners.geojobs.service;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.job.model.Status;
import app.bpartners.geojobs.repository.model.DetectionAddressConversionTask;
import app.bpartners.geojobs.service.dashboard.AreaPictureApi;
import app.bpartners.geojobs.service.dashboard.mapper.AreaPictureDetailsMapper;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionAddressConversionTaskConsumer
    implements BiConsumer<DetectionAddressConversionTask, String> {
  private final AreaPictureApi areaPictureApi;
  private final AreaPictureDetailsMapper areaPictureDetailsMapper;

  @Override
  public void accept(DetectionAddressConversionTask task, String e2ApiKey) {
    var address = task.getAddress();
    var crupdateAreaPictureDetails = areaPictureDetailsMapper.toCrupdateAreaPictureDetails(address);

    var areaPictureId = randomUUID().toString();
    var areaPictureDetails =
        areaPictureApi.crupdateAreaPictureDetails(
            areaPictureId, crupdateAreaPictureDetails, e2ApiKey);

    var feature = areaPictureDetailsMapper.toFeature(areaPictureDetails);
    task.setFeature(feature);
    task.setLayer(areaPictureDetails.actualLayer().name());
  }

  public static DetectionAddressConversionTask withNewStatus(
      DetectionAddressConversionTask task,
      Status.ProgressionStatus progression,
      Status.HealthStatus health,
      String message) {
    return (DetectionAddressConversionTask)
        task.hasNewStatus(
            Status.builder()
                .progression(progression)
                .health(health)
                .creationDatetime(now())
                .message(message)
                .build());
  }
}
