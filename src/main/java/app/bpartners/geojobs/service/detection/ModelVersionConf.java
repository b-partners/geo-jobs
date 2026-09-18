package app.bpartners.geojobs.service.detection;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
public class ModelVersionConf {
  private static final String MODEL_VERSION_CONF_PREFIX = "conf/%s/model-versions/";
  private static final String CONF_FILE_EXTENSION = ".json";
  private static final Comparator<String> BY_SEMANTIC_VERSION = comparingSemanticVersion();

  private final CustomBucketComponent bucketComponent;
  private final ObjectMapper om;
  private final String env = System.getenv("ENV");
  private final Map<String, Map<ModelName, List<TileDetectorUrl>>> confByVersion =
      new ConcurrentHashMap<>();
  private volatile Set<String> supportedVersions;

  public ModelVersionConf(CustomBucketComponent bucketComponent, ObjectMapper om) {
    this.bucketComponent = bucketComponent;
    this.om = om;
  }

  public String resolveVersion(String requestedVersion) {
    if (requestedVersion == null || requestedVersion.isBlank()) {
      return earliestVersion();
    }
    if (!supportedVersions().contains(requestedVersion)) {
      throw new BadRequestException("Unsupported model version " + requestedVersion);
    }
    return requestedVersion;
  }

  public boolean hasConfiguration(ModelName modelName, String requestedVersion) {
    if (requestedVersion == null || requestedVersion.isBlank()) {
      return true;
    }
    return supportedVersions().contains(requestedVersion)
        && confByModel(requestedVersion).containsKey(modelName);
  }

  public Set<String> getAvailableVersions(ModelName modelName) {
    var availableVersions = new TreeSet<String>(BY_SEMANTIC_VERSION);
    for (var version : supportedVersions()) {
      if (confByModel(version).containsKey(modelName)) {
        availableVersions.add(version);
      }
    }
    return availableVersions;
  }

  public List<TileDetectorUrl> getTileDetectorUrls(ModelName modelName, String requestedVersion) {
    var version = resolveVersion(requestedVersion);
    var urls = confByModel(version).get(modelName);
    if (urls == null) {
      throw new BadRequestException(
          "Model " + modelName.getValue() + " has no configuration for version " + version);
    }
    return urls;
  }

  private String earliestVersion() {
    return supportedVersions().stream()
        .min(BY_SEMANTIC_VERSION)
        .orElseThrow(
            () ->
                new BadRequestException(
                    "No model version configuration found under "
                        + String.format(MODEL_VERSION_CONF_PREFIX, env)));
  }

  private Set<String> supportedVersions() {
    if (supportedVersions == null) {
      supportedVersions = listSupportedVersions();
    }
    return supportedVersions;
  }

  private Set<String> listSupportedVersions() {
    var prefix = String.format(MODEL_VERSION_CONF_PREFIX, env);
    return bucketComponent
        .listObjects(bucketComponent.getBucketConf().getBucketName(), prefix)
        .stream()
        .map(S3Object::key)
        .map(key -> key.substring(prefix.length()))
        .filter(name -> name.endsWith(CONF_FILE_EXTENSION) && !name.contains("/"))
        .map(name -> name.substring(0, name.length() - CONF_FILE_EXTENSION.length()))
        .filter(name -> !name.isBlank())
        .collect(Collectors.toCollection(() -> new TreeSet<>(BY_SEMANTIC_VERSION)));
  }

  private Map<ModelName, List<TileDetectorUrl>> confByModel(String version) {
    return confByVersion.computeIfAbsent(version, this::load);
  }

  @SneakyThrows
  private Map<ModelName, List<TileDetectorUrl>> load(String version) {
    var bucketKey = String.format(MODEL_VERSION_CONF_PREFIX, env) + version + CONF_FILE_EXTENSION;
    var file = bucketComponent.download(bucketComponent.getBucketConf().getBucketName(), bucketKey);
    var rawConf = readString(file.toPath());

    deleteIfExists(file.toPath());

    Map<String, List<TileDetectorUrl>> rawConfByModel =
        om.readValue(rawConf, new TypeReference<>() {});

    Map<ModelName, List<TileDetectorUrl>> confByModel = new EnumMap<>(ModelName.class);
    rawConfByModel.forEach(
        (modelName, urls) -> confByModel.put(ModelName.fromValue(modelName), urls));
    return confByModel;
  }

  private static Comparator<String> comparingSemanticVersion() {
    return (left, right) -> {
      var leftParts = Arrays.stream(left.split("\\.")).map(Integer::parseInt).toList();
      var rightParts = Arrays.stream(right.split("\\.")).map(Integer::parseInt).toList();
      for (var i = 0; i < Math.max(leftParts.size(), rightParts.size()); i++) {
        var leftPart = i < leftParts.size() ? leftParts.get(i) : 0;
        var rightPart = i < rightParts.size() ? rightParts.get(i) : 0;
        if (leftPart != rightPart) {
          return Integer.compare(leftPart, rightPart);
        }
      }
      return 0;
    };
  }
}
