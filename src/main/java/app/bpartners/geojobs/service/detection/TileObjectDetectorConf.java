package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TileObjectDetectorConf {
  private final BucketComponent bucketComponent;

  public String getTileDetectionApiUrls() {
    File configFile = null;
    try {
      configFile = getTileDetectionApiUrlsFile();
      return Files.readString(configFile.toPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (configFile != null) {
        configFile.delete();
      }
    }
  }

  public File getTileDetectionApiUrlsFile() {
    var filename = "conf/tileDetectionApiUrls.json";
    return bucketComponent.download(filename);
  }
}
