package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

import app.bpartners.annotator.endpoint.rest.model.AnnotatedTask;
import app.bpartners.annotator.endpoint.rest.model.Annotation;
import app.bpartners.annotator.endpoint.rest.model.AnnotationBatch;
import app.bpartners.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
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

  public List<Label> extractLabelsFromTasks(List<AnnotatedTask> annotatedTasks) {
    return annotatedTasks.stream()
        .map(AnnotatedTask::getAnnotationBatch)
        .map(AnnotationBatch::getAnnotations)
        .map(a -> a.stream().map(Annotation::getLabel).collect(toSet()))
        .reduce(
            new HashSet<>(),
            (acc, val) -> {
              acc.addAll(val);
              return acc;
            })
        .stream()
        .toList();
  }
}
