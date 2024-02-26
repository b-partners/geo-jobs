package app.bpartners.geojobs.service.tiling.downloader;

import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.TilesDownloader;
import java.io.File;
import java.nio.file.Path;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Conditional(IsPreprodEnvCondition.class)
public class MockedTilesDownloader implements TilesDownloader {
  @Override
  public File apply(ParcelContent parcelContent) {
    Path mockFilePath =
        Path.of(new ClassPathResource("mock/tile-downloader-mock-result").getPath());
    return mockFilePath.toFile();
  }
}
