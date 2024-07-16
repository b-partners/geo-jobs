package app.bpartners.geojobs.unit;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static app.bpartners.geojobs.endpoint.rest.model.MultiPolygon.TypeEnum.MULTIPOLYGON;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeoJsonMapperTest {
  private final GeoJsonMapper subject = new GeoJsonMapper();

  private Feature annotation() {
    Feature feature = new Feature();
    var coordinates =
        List.of(
            List.of(
                List.of(
                    List.of(
                        BigDecimal.valueOf(322.36584784164086),
                        BigDecimal.valueOf(134.31910326735036)),
                    List.of(
                        BigDecimal.valueOf(385.8621512043883),
                        BigDecimal.valueOf(0.0)),
                    List.of(
                        BigDecimal.valueOf(66.66666666666667),
                        BigDecimal.valueOf(0.0)),
                    List.of(
                        BigDecimal.valueOf(61.05413784879562),
                        BigDecimal.valueOf(19.5373241116146)),
                    List.of(
                        BigDecimal.valueOf(322.36584784164086),
                        BigDecimal.valueOf(134.31910326735036)))));

    MultiPolygon multiPolygon = new MultiPolygon().coordinates(coordinates);
    multiPolygon.setType(MULTIPOLYGON);
    feature.setGeometry(multiPolygon);
    feature.setId(randomUUID().toString());
    return feature;
  }

  @Test
  void annotation_to_geo_json() {
    List<GeoJson.GeoFeature> actual = subject.toGeoFeatures(538559, 373791, 20, 1024, List.of(annotation()));

    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }
}
