package app.bpartners.geojobs.service;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectionImageAttributeRetriever implements Function<Detection, String> {
  private final BucketComponent bucketComponent;

  @Override
  public String apply(Detection detection) {
    var imageFileKey = detection.getImageFileKey();
    var polygonGeoJsonZone = detection.getPolygonGeoJsonZone();
    if (imageFileKey == null && polygonGeoJsonZone != null) {
      try {
        return bucketComponent.presign("zone_images/" + detection.getId() + ".jpg");
      } catch (RuntimeException e) {
        return null;
      }
    }
    return imageFileKey == null ? null : bucketComponent.presign(imageFileKey);
  }
}
