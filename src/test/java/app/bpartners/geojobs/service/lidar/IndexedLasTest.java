package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.service.lidar.model.LidarClass.OTHER;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.service.lidar.model.IndexedLas;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
class IndexedLasTest {

  @Test
  void polygon_contains_LAZPoints() {
    var lasFile =
        new File(getClass().getClassLoader().getResource("las/2019_saipan_waveform.laz").getFile());
    var indexedLas = new IndexedLas(lasFile, Set.of(OTHER));

    var x0 = 370300.7;
    var y0 = 1675206.6;
    assertEquals(
        2_480,
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

  @Test
  @Disabled
  void large_laz_is_supported() {
    var lasFile =
        new File(
            getClass()
                .getClassLoader()
                .getResource("las/LHD_FXX_0809_6306_PTS_LAMB93_IGN69.copc.laz")
                .getFile());

    var indexedLas = new IndexedLas(lasFile, Set.of(OTHER));

    var x0 = 80903100;
    var y0 = 630565435;
    log.info("Containment test...");
    assertEquals(
        18,
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
    log.info("Containment test... done");
  }
}
