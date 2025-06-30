package app.bpartners.geojobs.endpoint.rest.postprocessing.tombe;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOMBE;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger;
import app.bpartners.geojobs.endpoint.rest.postprocessing.MergeConf;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.area.Area;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public class TombeTest {
  private final TilingConf tilingConf = new TilingConf(20, 1024);
  private final UnionConf unionConf = new UnionConf(5);
  /*
   * Tombe : 4_000
   * Pathway : 20_000
   * Pool: 4_000
   */
  private final MergeConf mergeConf = new MergeConf(4000, 0.6);
  private final PrettyConf prettyConf = new PrettyConf(1);
  private final BoundaryMerger boundaryMerger = new BoundaryMerger(4000, 20);
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/dijon.json");

  public static Set<LatLonPolygon> invert(Set<LatLonPolygon> noSuperpositionPolygons) {
    return noSuperpositionPolygons.stream()
        .map(
            p -> {
              var coords =
                  Arrays.stream(p.polygon().getCoordinates())
                      .map(c -> new Coordinate(c.y, c.x))
                      .toArray(Coordinate[]::new);
              var initialLength = coords.length;
              if (!coords[0].equals(coords[initialLength - 1])) {
                coords = Arrays.copyOf(coords, initialLength + 1);
                coords[initialLength] = coords[0];
              }
              var polygon = geometryFactory.createPolygon(coords);
              polygon.setUserData(p.polygon().getUserData());
              return new LatLonPolygon(polygon);
            })
        .collect(toSet());
  }

  @Test
  void run() {
    var tiledPolygons = polygonProvider.getTiledPolygons(false);

    var merged = boundaryMerger.apply(tiledPolygons, TOMBE);

    // new Geojson(merged).saveAsFile("tombes_postprocessed_v7.geojson");
  }

  private Set<LatLonPolygon> filterByMinArea(Set<LatLonPolygon> tiledPolygons, Area tombeMinArea) {
    return tiledPolygons.stream()
        .flatMap(p -> emptyIfTooSmall(p, tombeMinArea).stream())
        .collect(toSet());
  }

  private Optional<LatLonPolygon> emptyIfTooSmall(LatLonPolygon latLonPolygon, Area tombeMinArea) {
    var p = latLonPolygon.polygon();
    if (new SquareDegree(p.getArea()).compareTo(tombeMinArea) < 0) {
      return Optional.empty();
    }
    return Optional.of(latLonPolygon);
  }
}
