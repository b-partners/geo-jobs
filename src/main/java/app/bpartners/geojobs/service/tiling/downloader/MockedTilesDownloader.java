package app.bpartners.geojobs.service.tiling.downloader;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.service.tiling.TilesDownloader;
import java.io.File;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Conditional(IsPreprodEnvCondition.class)
public class MockedTilesDownloader implements TilesDownloader {
  @Override
  public File apply(ParcelContent parcelContent) {
    throw new NotImplementedException("TODO: return mocked unzipped file");
  }
}
