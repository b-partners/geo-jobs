package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.POOL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GeoJsonMapperTest {
  private final GeoJsonMapper subject = new GeoJsonMapper();

  public static Feature feature() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(BigDecimal.valueOf(600), BigDecimal.valueOf(136.5)),
                    List.of(BigDecimal.valueOf(566), BigDecimal.valueOf(800.54)),
                    List.of(BigDecimal.valueOf(1022), BigDecimal.valueOf(1010)),
                    List.of(BigDecimal.valueOf(6), BigDecimal.valueOf(43)))));
    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    multiPolygon.setType(MULTIPOLYGON);
    feature.setGeometry(multiPolygon);
    feature.setId(randomUUID().toString());
    return feature;
  }

  public static DetectedObject detectedObject() {
    return DetectedObject.builder()
        .id(randomUUID().toString())
        .feature(feature())
        .computedConfidence(0.95)
        .detectedObjectTypes(List.of(DetectableObjectType.builder().detectableType(POOL).build()))
        .build();
  }

  @Test
  void annotation_to_geo_json() {
    List<GeoJson.GeoFeature> actual =
        subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(detectedObject()));

    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }
}
