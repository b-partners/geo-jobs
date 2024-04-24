package app.bpartners.geojobs.service.annotator;

import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.gen.annotator.endpoint.rest.model.*;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.DetectedTile;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LabelExtractor implements Function<DetectableType, Label> {
  @Override
  public Label apply(DetectableType detectableType) {
    return new Label()
        .id(randomUUID().toString())
        .color(getColorFromDetectedType(detectableType))
        .name(detectableType.name());
  }

  public List<Label> createUniqueLabelListFrom(List<DetectedTile> tiles) {
    return tiles.stream()
        .map(DetectedTile::getDetectedObjects)
        .flatMap(Collection::stream)
        .map(DetectedObject::getDetectableObjectType)
        .distinct()
        .map(this)
        .toList();
  }

  public Label findLabelByNameFromList(List<Label> source, String labelName) {
    return source.stream()
        .filter(
            label -> {
              assert label.getName() != null;
              return label.getName().equals(labelName);
            })
        .findFirst()
        .orElseThrow(
            () ->
                new ApiException(
                    SERVER_EXCEPTION,
                    "could not find label " + labelName + " for the current job."));
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    return switch (detectableType) {
      case ROOF -> "#DFFF00";
      case SOLAR_PANEL -> "#0E4EB3";
      case POOL -> "#0DCBD2";
      case PATHWAY -> "#F5F586";
      case TREE -> "#4BFF33";
      default -> throw new IllegalArgumentException("unexpected value");
    };
  }

  public List<Label> extractLabelsFromTasks(List<CreateAnnotatedTask> annotatedTasks) {
    return annotatedTasks.stream()
        .map(CreateAnnotatedTask::getAnnotationBatch)
        .map(CreateAnnotationBatch::getAnnotations)
        .map(a -> a.stream().map(AnnotationBaseFields::getLabel).collect(toSet()))
        .reduce(
            new HashSet<>(),
            (acc, val) -> {
              acc.addAll(val);
              return acc;
            })
        .stream()
        .filter(distinctByKeys(Label::getName))
        .toList();
  }

  // TODO: set it into a specific function
  @SafeVarargs
  private static <T> Predicate<T> distinctByKeys(final Function<? super T, ?>... keyExtractors) {
    final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();
    return elt -> {
      final List<?> keys =
          Arrays.stream(keyExtractors).map(key -> key.apply(elt)).collect(Collectors.toList());
      return seen.putIfAbsent(keys, Boolean.TRUE) == null;
    };
  }
}
