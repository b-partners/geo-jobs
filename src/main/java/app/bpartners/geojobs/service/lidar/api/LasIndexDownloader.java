package app.bpartners.geojobs.service.lidar.api;

import java.io.File;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LasIndexDownloader {
  public Optional<File> download(File lasFile, String fileUrl, File directory) {
    log.info("Start downloading lasIndex for FileURL={}", fileUrl);
    // TODO
    log.info("Finished downloading lasIndex for FileURL={}", fileUrl);
    return Optional.empty();
  }
}
