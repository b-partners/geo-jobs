package app.bpartners.geojobs.model.geometry.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.VGGLinesContinuer;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import org.junit.jupiter.api.Test;

public class VGGLinesContinuerTest {
  PolygonProvider pathProvider =
      new PolygonProvider(
          "/geometry/vgg/pathway.json", new IntXY(538860, 367571), new IntXY(1024, 1024));
  VGGLinesContinuer subject = new VGGLinesContinuer(false, 1024);

  @Test
  void vgg_to_tiled_polygon() {
    var vgg = pathProvider.getVggAnnotations();

    var actual = subject.apply(vgg);

    assertFalse(actual.isEmpty());
    assertEquals(8, actual.size());
    assertTrue(actual.stream().allMatch(tiledPolygon -> tiledPolygon.polygon() != null));
    assertTrue(actual.stream().allMatch(tiledPolygon -> tiledPolygon.originTile() != null));
    assertTrue(actual.stream().allMatch(tiledPolygon -> tiledPolygon.tilingConf() != null));
  }
}
