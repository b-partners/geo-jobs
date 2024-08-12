package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.TilingTask;
import app.bpartners.geojobs.service.tiling.downloader.MockedTilesDownloader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class TilingTaskConsumerWithMockedDownloaderTest {
  FileWriter fileWriter = mock();

  @Test
  void can_consume_with_no_error() throws IOException {
    var subject = new TilingTaskConsumer(new MockedTilesDownloader(5_000, 0, fileWriter), mock());

    when(fileWriter.createSecureTempDirectory(any())).thenReturn(new File("").toPath());

    subject.accept(
        new TilingTask()
            .toBuilder()
                .parcels(
                    List.of(new Parcel().toBuilder().parcelContent(new ParcelContent()).build()))
                .build());
  }

  @Test
  void can_consume_with_some_errors() {
    var subject = new TilingTaskConsumer(new MockedTilesDownloader(2_000, 50, fileWriter), mock());

    try {
      for (int i = 0; i < 10; i++) {
        subject.accept(
            new TilingTask()
                .toBuilder()
                    .parcels(
                        List.of(
                            new Parcel().toBuilder().parcelContent(new ParcelContent()).build()))
                    .build());
      }
    } catch (Exception e) {
      return;
    }
    fail();
  }
}
