package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TileObjectDetectorConf {
  private final BucketComponent bucketComponent;
  private String tileDetectionApiUrls;

  public TileObjectDetectorConf(BucketComponent bucketComponent) {
    this.bucketComponent = bucketComponent;
    this.tileDetectionApiUrls = null;
  }

  public String getTileDetectionApiUrls() {
    if (this.tileDetectionApiUrls == null) {
      this.tileDetectionApiUrls = getFromS3();
    }
    return this.tileDetectionApiUrls;
  }

  public String getFromS3() {
    try {
      File configFile = bucketComponent.download("conf/tileDetectionApiUrls.json");
      if (configFile != null) {
        var apiUrls = Files.readString(configFile.toPath());
        Files.deleteIfExists(configFile.toPath());
        return apiUrls;
      }
      log.error("Tile object detection api urls not found.");
      return "";
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
