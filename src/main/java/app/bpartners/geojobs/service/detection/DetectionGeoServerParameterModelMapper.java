package app.bpartners.geojobs.service.detection;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerProperties;
import app.bpartners.geojobs.repository.model.detection.GeoServerParameterStringMapValue;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class DetectionGeoServerParameterModelMapper
    implements Function<Object, List<GeoServerParameterStringMapValue>> {
  @SneakyThrows
  @Override
  public List<GeoServerParameterStringMapValue> apply(Object geoServerProperties) {
    List<GeoServerParameterStringMapValue> GeoServerParameterStringMapValues = new ArrayList<>();
    List<Field> fields = List.of(GeoServerProperties.class.getDeclaredFields());
    for (int i = 1; i < fields.size() - 1; i += 2) {
      Field fieldName = fields.get(i);
      Field fieldValue = fields.get(i + 1);
      fieldName.setAccessible(true);
      fieldValue.setAccessible(true);
      var name = fieldName.getName();
      var value = fieldValue.getName();
      GeoServerParameterStringMapValues.add(new GeoServerParameterStringMapValue(name, value));
    }
    return GeoServerParameterStringMapValues;
  }
}
