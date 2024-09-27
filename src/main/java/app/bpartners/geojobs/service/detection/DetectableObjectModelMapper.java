package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.rest.model.BPToitureModel;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectModelStringMapValue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class DetectableObjectModelMapper
    implements Function<Object, List<DetectableObjectModelStringMapValue>> {
  private static final String BP_MODEL_NAME_ATTRIBUTE_NAME = "modelName";
  private static final String DEFAULT_MODEL_NAME_KEY = "Nom du modèle";

  @SneakyThrows
  @Override
  public List<DetectableObjectModelStringMapValue> apply(Object modelInstance) {
    var detectableObjectStringValues = new ArrayList<DetectableObjectModelStringMapValue>();
    Field[] fields = modelInstance.getClass().getFields();
    for (Field field : fields) {
      field.setAccessible(true);
      String fieldName = field.getName();
      if (field.getType().equals(Boolean.class)) {
        var value = field.getBoolean(modelInstance) ? "oui" : "non";
        detectableObjectStringValues.add(new DetectableObjectModelStringMapValue(fieldName, value));
      } else if (BP_MODEL_NAME_ATTRIBUTE_NAME.equals(fieldName)
          && field.getType().equals(BPToitureModel.ModelNameEnum.class)) {
        detectableObjectStringValues.add(
            new DetectableObjectModelStringMapValue(
                DEFAULT_MODEL_NAME_KEY,
                ((BPToitureModel.ModelNameEnum) field.get(modelInstance)).getValue()));
      }
    }
    return detectableObjectStringValues;
  }
}
