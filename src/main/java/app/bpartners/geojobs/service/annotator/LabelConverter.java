package app.bpartners.geojobs.service.annotator;

import static java.util.UUID.randomUUID;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class LabelConverter implements Function<DetectableType, Label> {

  @Override
  public Label apply(DetectableType detectableType) {
    return new Label()
        .id(randomUUID().toString())
        .color(getColorFromDetectedType(detectableType))
        .name(detectableType.name());
  }

  private static String getColorFromDetectedType(DetectableType detectableType) {
    return switch (detectableType) {
      case TOITURE_REVETEMENT -> "#DFFF00";
      case PANNEAU_PHOTOVOLTAIQUE -> "#0E4EB3";
      case PISCINE -> "#0DCBD2";
      case PASSAGE_PIETON -> "#F5F586";
      case ARBRE -> "#4BFF33";
      default -> throw new IllegalArgumentException("unexpected value");
    };
  }
}
