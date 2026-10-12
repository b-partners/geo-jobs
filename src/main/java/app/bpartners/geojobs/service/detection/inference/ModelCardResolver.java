package app.bpartners.geojobs.service.detection.inference;

import app.bpartners.geojobs.repository.model.ArcgisImageZoom;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

@Component
public class ModelCardResolver {
  public static final int DEFAULT_TILING_ZOOM = ArcgisImageZoom.HOUSES_0.getZoomLevel();

  private final InferenceModelCardConf conf;
  private final ModelCardRegistry registry;

  public ModelCardResolver(InferenceModelCardConf conf, ModelCardRegistry registry) {
    this.conf = conf;
    this.registry = registry;
  }

  public int tilingZoomOf(DetectableType objectType) {
    var mapping = mappingsByType().get(objectType);
    return mapping == null ? DEFAULT_TILING_ZOOM : mapping.zoomLevel();
  }

  public List<Integer> tilingZoomLevels(List<DetectableObjectConfiguration> objectConfigurations) {
    var mappingsByType = mappingsByType();
    var zoomLevels =
        objectConfigurations.stream()
            .map(
                configuration -> {
                  var mapping = mappingsByType.get(configuration.getObjectType());
                  return mapping == null ? DEFAULT_TILING_ZOOM : mapping.zoomLevel();
                })
            .distinct()
            .sorted()
            .toList();
    return zoomLevels.isEmpty() ? List.of(DEFAULT_TILING_ZOOM) : zoomLevels;
  }

  public Resolution resolve(
      List<DetectableObjectConfiguration> objectConfigurations,
      int tileZoom,
      Predicate<DeployedModelCard> compatible) {
    var mappingsByType = mappingsByType();
    var modelCardsById = new LinkedHashMap<String, DeployedModelCard>();
    var usableModelCardsById = new LinkedHashMap<String, ResolvedModelCard>();
    var unmappedTypes = new LinkedHashSet<DetectableType>();
    var unsupportedTypes = new LinkedHashSet<DetectableType>();
    for (var objectConfiguration : objectConfigurations) {
      var objectType = objectConfiguration.getObjectType();
      var mapping = mappingsByType.get(objectType);
      if (mapping == null) {
        if (tileZoom == DEFAULT_TILING_ZOOM) {
          unmappedTypes.add(objectType);
        }
        continue;
      }
      if (mapping.zoomLevel() != tileZoom) {
        continue;
      }
      var modelCard =
          modelCardsById.computeIfAbsent(
              mapping.modelCardId(), id -> findRunnable(objectType, id));
      if (compatible.test(modelCard)) {
        usableModelCardsById.putIfAbsent(
            mapping.modelCardId(),
            new ResolvedModelCard(modelCard, mapping.zoomLevel(), mapping.spatialResolution()));
      } else {
        unsupportedTypes.add(objectType);
      }
    }
    return new Resolution(
        List.copyOf(usableModelCardsById.values()),
        Set.copyOf(unmappedTypes),
        Set.copyOf(unsupportedTypes));
  }

  private LinkedHashMap<DetectableType, ModelCardMapping> mappingsByType() {
    var mappingsByType = new LinkedHashMap<DetectableType, ModelCardMapping>();
    conf.getMappings().forEach(mapping -> mappingsByType.putIfAbsent(mapping.objectType(), mapping));
    return mappingsByType;
  }

  private DeployedModelCard findRunnable(DetectableType objectType, String modelCardId) {
    return registry
        .findById(modelCardId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Model card "
                        + modelCardId
                        + " mapped to "
                        + objectType
                        + " is not deployed or not runnable"));
  }

  public record Resolution(
      List<ResolvedModelCard> modelCards,
      Set<DetectableType> unmappedTypes,
      Set<DetectableType> unsupportedTypes) {}
}
