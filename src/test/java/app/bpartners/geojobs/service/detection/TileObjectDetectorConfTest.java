package app.bpartners.geojobs.service.detection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.nio.file.Files;

class TileObjectDetectorConfIT extends FacadeIT {
  @Autowired TileObjectDetectorConf subject;

  @MockBean BucketComponent bucketComponentMock;
  File mockFile;

  @SneakyThrows
  @BeforeEach
  void setUp() {

    mockFile = File.createTempFile("test", ".json");
    String mockContent = "{\"urls\": [\"http://mock-api-url.com\"]}";
    Files.writeString(mockFile.toPath(), mockContent);
  }

  @SneakyThrows
  @Test
  void get_tile_detection_api_urls() {
    when(bucketComponentMock.download(any(String.class))).thenReturn(mockFile);
    var actual = subject.getTileDetectionApiUrls();

    assertNotNull(actual);
    assertFalse(actual.isBlank());
  }
}
