package app.bpartners.geojobs.service.detection.inference;

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
  private final InferenceModelCardConf conf;
  private final ModelCardRegistry registry;

  public ModelCardResolver(InferenceModelCardConf conf, ModelCardRegistry registry) {
    this.conf = conf;
    this.registry = registry;
  }

  public Resolution resolve(
      List<DetectableObjectConfiguration> objectConfigurations,
      Predicate<DeployedModelCard> compatible) {
    var modelCardIdByType = new LinkedHashMap<DetectableType, String>();
    conf.getMappings()
        .forEach(
            mapping -> modelCardIdByType.putIfAbsent(mapping.objectType(), mapping.modelCardId()));

    var modelCardsById = new LinkedHashMap<String, DeployedModelCard>();
    var usableModelCardsById = new LinkedHashMap<String, DeployedModelCard>();
    var unmappedTypes = new LinkedHashSet<DetectableType>();
    var unsupportedTypes = new LinkedHashSet<DetectableType>();
    for (var objectConfiguration : objectConfigurations) {
      var objectType = objectConfiguration.getObjectType();
      var modelCardId = modelCardIdByType.get(objectType);
      if (modelCardId == null) {
        unmappedTypes.add(objectType);
        continue;
      }
      var modelCard =
          modelCardsById.computeIfAbsent(modelCardId, id -> findRunnable(objectType, id));
      if (compatible.test(modelCard)) {
        usableModelCardsById.putIfAbsent(modelCardId, modelCard);
      } else {
        unsupportedTypes.add(objectType);
      }
    }
    return new Resolution(
        List.copyOf(usableModelCardsById.values()),
        Set.copyOf(unmappedTypes),
        Set.copyOf(unsupportedTypes));
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
      List<DeployedModelCard> modelCards,
      Set<DetectableType> unmappedTypes,
      Set<DetectableType> unsupportedTypes) {}
}
