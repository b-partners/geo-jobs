package app.bpartners.geojobs.model.geometry.route;

import static java.lang.Math.PI;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.VGGLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.quadrilateral.model.AlphaConf;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class VGGLinesContinuerTest {
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/line-pathway.json");

  VGGLinesContinuer subject =
      new VGGLinesContinuer(routesContinuationConf(), new TilingConf(20, 1_024), false);

  @Test
  void generate_continued_geojson_from_vgg() throws IOException {
    var vgg = polygonProvider.getVggAnnotations();

    var actual = subject.apply(vgg);

    assertEquals("", actual.stringValue());
  }

  private static RoutesContinuationConf routesContinuationConf() {
    var alphaConf = new AlphaConf(0.5, 1);
    var unionConf = new UnionConf(0);
    var continuationConf = new ContinuationConf(PI / 12, PI / 6, 500);
    var prettyConf = new PrettyConf(0);
    return new RoutesContinuationConf(alphaConf, unionConf, continuationConf, prettyConf);
  }
}
