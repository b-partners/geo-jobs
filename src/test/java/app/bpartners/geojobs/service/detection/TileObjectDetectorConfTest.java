package app.bpartners.geojobs.service.detection;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class TileObjectDetectorConfTest extends FacadeIT {
  @Autowired TileObjectDetectorConf subject;
  @MockBean BucketComponent bucketComponentMock;

  @Test
  void get_tile_detection_api_urls() {
    var mockFile =
        new File(
            requireNonNull(
                    getClass().getClassLoader().getResource("conf/tileObjectDetectorApiUrls.json"))
                .getFile());

    when(bucketComponentMock.download(any(String.class))).thenReturn(mockFile);
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
