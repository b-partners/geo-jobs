package app.bpartners.geojobs.service.geojson;

import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GeoJson implements Serializable {
  private static final String DEFAULT_TYPE = "FeatureCollection";
  private final String type;
  private final CRS crs;
  private final List<GeoFeature> features;

  public GeoJson(List<GeoFeature> features) {
    this.type = DEFAULT_TYPE;
    this.crs = new CRS();
    this.features = features;
  }

  @AllArgsConstructor
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  public static class CRS implements Serializable {
    private static final String DEFAULT_CSR_TYPE = "name";
    private static final Map<String, String> DEFAULT_CSR_PROPERTIES =
        Map.of("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
    private final String type;
    private final Map<String, String> properties;

    public CRS() {
      this.type = DEFAULT_CSR_TYPE;
      this.properties = DEFAULT_CSR_PROPERTIES;
    }
  }

  @AllArgsConstructor
  @Getter
  @Setter
  @ToString
  @EqualsAndHashCode
  public static class GeoFeature implements Serializable {
    private static final String DEFAULT_FEATURE_TYPE = "Feature";
    private Map<String, String> properties;
    private String type;
    private MultiPolygon geometry;

    public GeoFeature(Map<String, String> properties, MultiPolygon geometry) {
      this.properties = properties;
      this.type = DEFAULT_FEATURE_TYPE;
      this.geometry = geometry;
    }
  }
}
