package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PASSAGE_PIETON;
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
    var coordinates =
        List.of(
            List.of(
                List.of(List.of(new BigDecimal("600.0"), new BigDecimal("136.5"))),
                List.of(List.of(new BigDecimal("566.0"), new BigDecimal("800.54"))),
                List.of(List.of(new BigDecimal("1022.0"), new BigDecimal("1010.0"))),
                List.of(List.of(new BigDecimal("6.0"), new BigDecimal("43.0")))));

    return new Feature().geometry(new MultiPolygon().coordinates(coordinates));
  }

  public static DetectedObject detectedObject() {
    return DetectedObject.builder()
        .id(randomUUID().toString())
        .feature(feature())
        .computedConfidence(0.95)
        .detectedObjectType(DetectableObjectType.builder().detectableType(PASSAGE_PIETON).build())
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
