package app.bpartners.geojobs.model.geometry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.polygon.Feature;
import app.bpartners.geojobs.model.geometry.polygon.FeatureListWithOffset;
import app.bpartners.geojobs.model.geometry.polygon.FeatureListWithoutOffset;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class FeatureProvider implements Function<Integer, Polygon> {

  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

  private final boolean withOffset;
  private final List<Feature> features;

  public FeatureProvider(String vggFilePath, boolean withOffset, IntXY imageResolution) {
    this.withOffset = withOffset;
    this.features = features(vggFilePath, imageResolution);
  }

  @Override
  public Polygon apply(Integer n) {
    return features.get(n).geometry();
  }

  public int featuresNb() {
    return features.size();
  }

  private List<Feature> features(String vggFilePath, IntXY imageResolution) {
    try (var vggStream = this.getClass().getResourceAsStream(vggFilePath)) {
      var vggAsString = new String(vggStream.readAllBytes(), UTF_8);
      var vgg = om.readValue(vggAsString, VGG.class);
      return withOffset
          ? new FeatureListWithOffset(vgg, imageResolution).get()
          : new FeatureListWithoutOffset(vgg, imageResolution).get();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<Polygon> getPolygons() {
    return features.stream().map(Feature::geometry).collect(toSet());
  }
}
