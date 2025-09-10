package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class TileObjectDetectorConf {
  private final BucketComponent bucketComponent;
  private final String tileDetectionApiUrls;

  public TileObjectDetectorConf(BucketComponent bucketComponent) {
    this.bucketComponent = bucketComponent;
    this.tileDetectionApiUrls = getFromS3();
  }

  public String getFromS3() {
    String apiUrls;
    try {
      File configFile = bucketComponent.download("conf/tileDetectionApiUrls.json");
      apiUrls = Files.readString(configFile.toPath());
      Files.deleteIfExists(configFile.toPath());
      return apiUrls;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
