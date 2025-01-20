package app.bpartners.geojobs.model.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

public class VGGFactoryTest {
  private final FeatureProvider featureProvider =
      new FeatureProvider("/geometry/vgg/pathway.json", false, new IntXY(1024, 1024));
  private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();
  private final VGGFactory subject = new VGGFactory();

  @Test
  void features_to_vgg_ok() {
    var features = featureProvider.getFeatures();

    var actual = subject.convert(features);

    assertEquals(5, actual.size());
    assertEquals(2, actual.get("5cm3346073745629231615_20_538860_367572.jpg").getRegions().size());
  }
}
