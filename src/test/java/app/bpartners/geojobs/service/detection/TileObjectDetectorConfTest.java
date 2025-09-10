package app.bpartners.geojobs.service.detection;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TileObjectDetectorConfTest {
  BucketComponent bucketComponentMock = mock(BucketComponent.class);
  TileObjectDetectorConf subject;

  @Test
  void get_tile_detection_api_urls() {
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

  @Test
  void get_tile_detection_api_urls_blank() {
    when(bucketComponentMock.download(any(String.class))).thenReturn(null);
    subject = new TileObjectDetectorConf(bucketComponentMock);

    var actual = subject.getTileDetectionApiUrls();

    assertTrue(actual.isBlank());
  }
}
