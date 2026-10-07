package app.bpartners.geojobs.service.detection.inference;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

class InferenceModelCardConfTest {
  BucketComponent bucketComponent = mock();
  InferenceModelCardConf subject = new InferenceModelCardConf(bucketComponent, new ObjectMapper());

  @Test
  @SneakyThrows
  void loads_mappings_once_and_deletes_downloaded_file() {
    var file =
        confFile(
            """
            [
              {"objectType": "HUMIDITE", "modelCardId": "damages-ignore-index-c4"},
              {"objectType": "TOMBE", "modelCardId": "cimetiere-tombe-v1.0", "unknown": 1}
            ]""");
    when(bucketComponent.download("conf/" + System.getenv("ENV") + "/inferenceModelCards.json"))
        .thenReturn(file);

    var first = subject.getMappings();
    var second = subject.getMappings();

    assertEquals(
        List.of(
            new ModelCardMapping(HUMIDITE, "damages-ignore-index-c4"),
            new ModelCardMapping(TOMBE, "cimetiere-tombe-v1.0")),
        first);
    assertEquals(first, second);
    assertFalse(file.exists());
    verify(bucketComponent, times(1)).download(any(String.class));
  }

  @Test
  void download_failed_and_throws_exception() {
    when(bucketComponent.download(any(String.class))).thenThrow(AwsServiceException.class);

    assertThrows(AwsServiceException.class, () -> subject.getMappings());
  }

  @SneakyThrows
  private File confFile(String content) {
    var file = Files.createTempFile("inferenceModelCards", ".json");
    Files.writeString(file, content);
    return file.toFile();
  }
}
