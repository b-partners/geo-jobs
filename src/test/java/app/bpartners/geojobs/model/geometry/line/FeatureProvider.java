package app.bpartners.geojobs.model.geometry.line;

import static java.nio.charset.StandardCharsets.UTF_8;

import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.polygon.FeatureList;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class FeatureProvider implements Function<Integer, Polygon> {

  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
  private static final String vggFilePath = "/geometry/vgg/dijon.json";

  private final FeatureList featureList;

  public FeatureProvider() {
    this.featureList = featureCollection();
  }

  @Override
  public Polygon apply(Integer n) {
    return featureList.get().get(n).geometry();
  }

  public int featuresNb() {
    return featureList.get().size();
  }

  private FeatureList featureCollection() {
    try (InputStream vggAnnotationResource = this.getClass().getResourceAsStream(vggFilePath)) {
      var vgg = new String(vggAnnotationResource.readAllBytes(), UTF_8);
      var vggAnnotations = om.readValue(vgg, VGG.class);
      return new FeatureList(vggAnnotations);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
