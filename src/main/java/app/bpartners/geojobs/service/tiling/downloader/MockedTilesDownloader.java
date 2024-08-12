package app.bpartners.geojobs.service.tiling.downloader;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.FalliblyDurableMockedFunction;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "tiles.downloader.mock.activated", havingValue = "true")
public class MockedTilesDownloader extends FalliblyDurableMockedFunction<ParcelContent, File>
    implements TilesDownloader {
  private final app.bpartners.geojobs.file.FileWriter fileWriter;

  public MockedTilesDownloader(
      @Value("${tiles.downloader.mock.maxCallDuration}") long maxCallDurationInMillis,
      @Value("${tiles.downloader.mock.failureRate}") double failureRate,
      app.bpartners.geojobs.file.FileWriter fileWriter) {
    super(Duration.ofMillis(maxCallDurationInMillis), failureRate);
    this.fileWriter = fileWriter;
  }

  @SneakyThrows
  @Override
  protected File successfulMockedApply(ParcelContent parcelContent) {
    var rootDir = fileWriter.createSecureTempDirectory("tiles").toFile();
    var zoomAndXDir = new File(rootDir.getAbsolutePath() + "/20/1");
    zoomAndXDir.mkdirs();

    var yName = ((int) (new SecureRandom().nextDouble() * 1000)) + ".txt";
    var yFile = new File(zoomAndXDir.getAbsolutePath() + "/" + yName);
    writeRandomContent(yFile);

    return rootDir;
  }

  private static void writeRandomContent(File file) throws IOException {
    try (FileWriter writer = new FileWriter(file)) {
      var content = randomUUID().toString();
      writer.write(content);
    }
  }
}
