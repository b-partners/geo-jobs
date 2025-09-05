package app.bpartners.geojobs.service.detection;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class TileObjectDetectorConfTest extends FacadeIT {
  ObjectMapper jsonMapper = new ObjectMapper();

  @Autowired TileObjectDetectorConf subject;

  @MockBean BucketComponent bucketComponentMock;
  File mockFile;

  @BeforeEach
  void setUp() {
    mockFile = new File(
        requireNonNull(
            getClass()
                .getClassLoader()
                .getResource("conf/tileObjectDetectorApiUrls.json"))
            .getFile());
    ;
  }

  @SneakyThrows
  @Test
  void get_tile_detection_api_urls() {
    when(bucketComponentMock.download(any(String.class))).thenReturn(mockFile);
    var actual = subject.getTileDetectionApiUrls();

    assertNotNull(actual);
    assertFalse(actual.isBlank());
    AtomicReference<JsonNode> content = new AtomicReference<>();
    assertDoesNotThrow(
        () -> {
          content.set(jsonMapper.readTree(actual));
        });
    assertTrue(content.get().get(0).has("url") && content.get().get(0).has("objectType"));
  }
}
