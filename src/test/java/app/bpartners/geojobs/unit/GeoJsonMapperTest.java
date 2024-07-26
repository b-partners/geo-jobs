package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.repository.model.detection.HumanDetectedObject;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GeoJsonMapperTest {
  private final GeoJsonMapper subject = new GeoJsonMapper();

  public static Polygon feature() {
    var coordinates =
        List.of(
            new Point().x(600.0).y(136.5),
            new Point().x(566.0).y(800.54),
            new Point().x(1022.0).y(1010.0),
            new Point().x(6.0).y(43.0));
    return new Polygon().points(coordinates);
  }

  public static HumanDetectedObject detectedObject() {
    return HumanDetectedObject.builder()
        .id(randomUUID().toString())
        .feature(feature())
        .confidence("0.95")
        .label(new Label().name("PATHWAY"))
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
