package app.bpartners.geojobs.service.detection;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.readString;

import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectModel;
import app.bpartners.geojobs.endpoint.rest.model.ModelName;
import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.service.detection.inference.ModelCardMapping;
import app.bpartners.geojobs.service.detection.inference.ModelCardMappingValidator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
  public static final String DEFAULT_VERSION = "1.1.0";
  public static final String LEGACY_VERSION = "legacy";
  private static final String MODEL_VERSION_CONF_PREFIX = "conf/%s/model-versions/";
  private static final String CONF_FILE_EXTENSION = ".json";
  private static final Comparator<String> BY_SEMANTIC_VERSION = comparingSemanticVersion();

  private final CustomBucketComponent bucketComponent;
  private final ObjectMapper om;
  private final String env = System.getenv("ENV");
  private final Map<String, Map<ModelName, ModelConf>> confByVersion = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> supportedVersionsByPrefix = new ConcurrentHashMap<>();

  public ModelVersionConf(CustomBucketComponent bucketComponent, ObjectMapper om) {
    this.bucketComponent = bucketComponent;
    this.om = om;
  }

  public String resolveVersion(String requestedVersion) {
    if (requestedVersion == null || requestedVersion.isBlank()) {
      return DEFAULT_VERSION;
    }
    if (LEGACY_VERSION.equals(requestedVersion)) {
      return earliestVersion();
    }
    if (!isSupported(requestedVersion)) {
      throw new BadRequestException("Unsupported model version " + requestedVersion);
    }
    return requestedVersion;
  }

  public boolean hasConfiguration(ModelName modelName, String requestedVersion) {
    var version =
        requestedVersion == null || requestedVersion.isBlank() ? DEFAULT_VERSION : requestedVersion;
    return isSupported(version) && confByModel(version).containsKey(modelName);
  }

  private boolean isSupported(String version) {
    return supportedVersions().stream().anyMatch(version::equals);
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
    var modelConf = confByModel(version).get(modelName);
    if (modelConf == null) {
      throw new BadRequestException(
          "Model " + modelName.getValue() + " has no configuration for version " + version);
    }
    return modelConf.lambdas();
  }

  public List<ModelCardMapping> getInferenceMappings(List<DetectableObjectModel> models) {
    var selectedModels =
        models == null
            ? List.<DetectableObjectModel>of()
            : models.stream().filter(model -> model.getModelName() != null).toList();
    var mappingsByTypeAndResolution = new LinkedHashMap<List<Object>, ModelCardMapping>();
    if (selectedModels.isEmpty()) {
      confByModel(resolveVersion(null))
          .values()
          .forEach(modelConf -> collect(modelConf.inference(), mappingsByTypeAndResolution));
    }
    for (var model : selectedModels) {
      var modelConf =
          confByModel(resolveVersion(model.getModelVersion())).get(model.getModelName());
      if (modelConf != null) {
        collect(modelConf.inference(), mappingsByTypeAndResolution);
      }
    }
    return List.copyOf(mappingsByTypeAndResolution.values());
  }

  private static void collect(
      List<ModelCardMapping> mappings, Map<List<Object>, ModelCardMapping> collected) {
    mappings.forEach(
        mapping ->
            collected.putIfAbsent(
                List.of(mapping.objectType(), mapping.spatialResolution()), mapping));
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
    return supportedVersionsByPrefix.computeIfAbsent(
        String.format(MODEL_VERSION_CONF_PREFIX, env), this::listSupportedVersions);
  }

  private Set<String> listSupportedVersions(String prefix) {
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

  private Map<ModelName, ModelConf> confByModel(String version) {
    return confByVersion.computeIfAbsent(version, this::load);
  }

  @SneakyThrows
  private Map<ModelName, ModelConf> load(String version) {
    var bucketKey = String.format(MODEL_VERSION_CONF_PREFIX, env) + version + CONF_FILE_EXTENSION;
    var file = bucketComponent.download(bucketComponent.getBucketConf().getBucketName(), bucketKey);
    var rawConf = readString(file.toPath());

    deleteIfExists(file.toPath());

    Map<String, List<ModelVersionEntry>> rawConfByModel =
        om.readValue(rawConf, new TypeReference<>() {});

    Map<ModelName, ModelConf> confByModel = new EnumMap<>(ModelName.class);
    rawConfByModel.forEach(
        (modelName, entries) ->
            confByModel.put(ModelName.fromValue(modelName), toModelConf(entries)));
    return confByModel;
  }

  private static ModelConf toModelConf(List<ModelVersionEntry> entries) {
    var inference = new ArrayList<ModelCardMapping>();
    var lambdas = new ArrayList<TileDetectorUrl>();
    for (var entry : entries) {
      var hasModelCard = entry.modelCardId() != null;
      var hasUrl = entry.url() != null;
      if (hasModelCard == hasUrl) {
        throw new IllegalStateException(
            "Model version entry must define either modelCardId or url: " + entry);
      }
      if (hasModelCard) {
        inference.add(
            new ModelCardMapping(
                entry.objectType(),
                entry.modelCardId(),
                entry.zoomLevel(),
                entry.spatialResolution(),
                entry.inferenceSpatialResolution()));
      } else {
        lambdas.add(
            TileDetectorUrl.builder()
                .objectType(entry.objectType())
                .url(entry.url())
                .version(entry.version())
                .build());
      }
    }
    ModelCardMappingValidator.validate(inference);
    return new ModelConf(List.copyOf(inference), List.copyOf(lambdas));
  }

  private record ModelConf(List<ModelCardMapping> inference, List<TileDetectorUrl> lambdas) {}

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
