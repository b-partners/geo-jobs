package app.bpartners.geojobs.service;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BucketServiceRoadContinuer {
  private final BucketComponent bucketComponent;

  public String getContinuedRoutePresignedUrl(File file, String adminApiKey) {
    bucketComponent.upload(file, adminApiKey);
    return String.valueOf(bucketComponent.presign(adminApiKey, Duration.ofHours(1)));
  }
}
