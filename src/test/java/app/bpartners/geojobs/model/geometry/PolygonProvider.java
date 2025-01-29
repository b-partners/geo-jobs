package app.bpartners.geojobs.model.geometry;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.feature.Feature;
import app.bpartners.geojobs.model.geometry.feature.FeatureListWithOffset;
import app.bpartners.geojobs.model.geometry.feature.FeatureListWithoutOffset;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.locationtech.jts.geom.Polygon;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class PolygonProvider implements Function<Integer, Polygon> {

  private static final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

  private final IntXY origin;
  private final List<Feature> features;

  public PolygonProvider(
      String vggFilePath, IntXY origin, IntXY imageResolution, boolean is_z_x_y_dot_filetype) {
    this.origin = origin;
    this.features = features(vggFilePath, imageResolution, is_z_x_y_dot_filetype);
  }

  public PolygonProvider(String vggFilePath, IntXY origin, IntXY imageResolution) {
    this.origin = origin;
    this.features = features(vggFilePath, imageResolution, false);
  }

  @Override
  public Polygon apply(Integer n) {
    return features.get(n).geometry();
  }

  public int featuresNb() {
    return features.size();
  }

  private List<Feature> features(
      String vggFilePath, IntXY imageResolution, boolean is_z_x_y_dot_filetype) {
    var resource = getClass().getResource(vggFilePath);
    try (var file = new RandomAccessFile(new File(resource.toURI()), "r")) {
      var channel = file.getChannel();
      var buffer = channel.map(READ_ONLY, 0, channel.size());
      var decoded = UTF_8.decode(buffer);
      var vggAsString = decoded.toString();
      var vgg = om.readValue(vggAsString, VGG.class);
      return origin == null
          ? new FeatureListWithoutOffset(vgg, imageResolution, is_z_x_y_dot_filetype).get()
          : new FeatureListWithOffset(vgg, imageResolution, is_z_x_y_dot_filetype, origin).get();
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public Set<Polygon> getPolygons() {
    return features.stream()
        .map(
            feature -> {
              var polygon = feature.geometry();
              var metadata = new HashMap<String, Object>();
              metadata.put("filename", feature.filename());
              metadata.put("label", feature.label());
              metadata.put("confidence", feature.confidence());
              polygon.setUserData(metadata);
              return polygon;
            })
        .collect(toSet());
  }
}
