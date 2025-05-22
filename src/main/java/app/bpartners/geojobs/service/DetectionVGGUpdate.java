package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionVGGUpdate implements BiFunction<VGG, Detection, Detection> {
  private static final String VGG_BUCKET_FOLDER = "vgg/";
  private final FileWriter fileWriter;
  private final BucketComponent bucketComponent;

  @Override
  public Detection apply(VGG vgg, Detection detection) {
    var zoneName = detection.getZoneName();
    var zoneDetectionJobId = detection.getZdjId();
    var fileKey = VGG_BUCKET_FOLDER + zoneDetectionJobId + "/" + zoneName + ".json";
    var vggAsByte = vgg.getBytes();
    var vggAsFile =
        fileWriter.write(vggAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
    bucketComponent.upload(vggAsFile, fileKey);
    return detection.toBuilder().vggFileKey(fileKey).build();
  }
}
