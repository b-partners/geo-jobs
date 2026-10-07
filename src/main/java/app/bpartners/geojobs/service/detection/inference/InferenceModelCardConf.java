package app.bpartners.geojobs.service.detection.inference;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class InferenceModelCardConf {
  private static final String BUCKET_KEY_INFERENCE_MODEL_CARDS_JSON =
      "conf/%s/inferenceModelCards.json";

  private final BucketComponent bucketComponent;
  private final ObjectMapper objectMapper;
  private final String env = System.getenv("ENV");
  private volatile List<ModelCardMapping> mappings;

  public InferenceModelCardConf(BucketComponent bucketComponent, ObjectMapper objectMapper) {
    this.bucketComponent = bucketComponent;
    this.objectMapper = objectMapper;
  }

  public List<ModelCardMapping> getMappings() {
    var local = mappings;
    if (local == null) {
      synchronized (this) {
        local = mappings;
        if (local == null) {
          local = loadFromS3();
          mappings = local;
        }
      }
    }
    return local;
  }

  @SneakyThrows
  private List<ModelCardMapping> loadFromS3() {
    var file = bucketComponent.download(String.format(BUCKET_KEY_INFERENCE_MODEL_CARDS_JSON, env));
    try {
      return List.copyOf(
          objectMapper.readValue(readString(file.toPath()), new TypeReference<>() {}));
    } finally {
      deleteIfExists(file.toPath());
    }
  }
}
