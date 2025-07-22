package app.bpartners.geojobs.model.las;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class IndexedLasTest {

  @Test
  void polygon_contains_LAZPoints() {
    var lasFile =
        new File(getClass().getClassLoader().getResource("las/2019_saipan_waveform.laz").getFile());

    var indexedLas = new IndexedLas(lasFile);

    var geometryFactory = new GeometryFactory();
    var x0 = 7030070;
    var y0 = 7520660;
    assertEquals(
        2,
        indexedLas
            .containedIn(
                geometryFactory.createPolygon(
                    new Coordinate[] {
                      new Coordinate(x0, y0),
                      new Coordinate(x0 + 100, y0),
                      new Coordinate(x0, y0 + 100),
                      new Coordinate(x0, y0),
                    }))
            .size());
  }
}
