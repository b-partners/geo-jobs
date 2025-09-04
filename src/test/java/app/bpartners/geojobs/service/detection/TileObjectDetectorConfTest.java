package app.bpartners.geojobs.service.detection;

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

class TileObjectDetectorConfIT extends FacadeIT {
  ObjectMapper jsonMapper = new ObjectMapper();

  @Autowired TileObjectDetectorConf subject;

  @MockBean BucketComponent bucketComponentMock;
  File mockFile;

  @SneakyThrows
  @BeforeEach
  void setUp() {
    mockFile = File.createTempFile("test", ".json");
    String mockContent =
        """
        [{
            "objectType": "PISCINE",
            "url": "dummy"
          },
          {
            "objectType": "PASSAGE_PIETON",
            "url": "dummy"
          }]
        """;
    Files.writeString(mockFile.toPath(), mockContent);
  }

  @AfterEach
  void tearDown() {
    mockFile.delete();
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
