package app.bpartners.geojobs.service.detection;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class TileObjectDetectorConfTest {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  TileObjectDetectorConf subject;

  @SneakyThrows
  @Test
  void get_tile_detection_api_urls() throws IOException {
    var mockFile =
        new File(
            requireNonNull(
                    getClass().getClassLoader().getResource("conf/tileObjectDetectorApiUrls.json"))
                .getFile());
    when(bucketComponentMock.download(any(String.class))).thenReturn(mockFile);

    subject = new TileObjectDetectorConf(bucketComponentMock);

    var expected =
        """
[
  {
    "objectType": "DUMMY",
    "url": "dummy"
  },
  {
    "objectType": "DUMMY",
    "url": "dummy"
  }
]""";
    var actual = new AtomicReference<>();
    assertDoesNotThrow(
        () -> {
          actual.set(subject.getTileDetectionApiUrls());
        });

    assertEquals(expected, actual.get());
  }
}
