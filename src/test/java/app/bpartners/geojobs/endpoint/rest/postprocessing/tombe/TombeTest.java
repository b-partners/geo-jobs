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
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class TombeTest {
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/dijon.json");
  private final TilingConf tilingConf = new TilingConf(20, 1024);
  private final UnionConf unionConf = new UnionConf(5);
  private final MergeConf mergeConf = new MergeConf(1, 1, 5);
  private final PrettyConf prettyConf = new PrettyConf(1);
  private final BoundaryMerger boundaryMerger =
      new BoundaryMerger(tilingConf, unionConf, mergeConf, prettyConf, 20);

  @Test
  void run() {
    var tiledPolygons = polygonProvider.getTiledPolygons(false);

    var merged = boundaryMerger.apply(tiledPolygons, TOMBE);

    var m2toDeg2 = 1E-11; // France
    var minArea = new SquareDegree(112 * m2toDeg2);
    var filteredByMinAreaPolygons = filterByMinArea(merged, minArea);

    var maxAllowedIoU = 0.6;
    var noSuperpositionPolygons = noSuperposition(filteredByMinAreaPolygons, maxAllowedIoU);

    // new Geojson(noSuperpositionPolygons).saveAsFile("tombes_postprocessed.geojson");
  }

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

  private Set<LatLonPolygon> noSuperposition(Set<LatLonPolygon> polygons, double maxAllowedIoU) {
    Map<LatLonPolygon, Boolean> isSuperposedByBiggerPolygon = new HashMap<>();
    var asList = new ArrayList<>(polygons);
    for (int i = 0; i < asList.size(); i++) {
      var pi = asList.get(i);
      var pip = pi.polygon();
      for (int j = i + 1; j < asList.size(); j++) {
        var pj = asList.get(j);
        var pjp = pj.polygon();
        if (pip.getArea() < pjp.getArea()) {
          if (isSuperposed(pip, pjp, maxAllowedIoU)) {
            isSuperposedByBiggerPolygon.put(pi, true);
          }
        } else {
          if (isSuperposed(pjp, pip, maxAllowedIoU)) {
            isSuperposedByBiggerPolygon.put(pj, true);
          }
        }
      }
    }

    Set<LatLonPolygon> res = new HashSet<>();
    for (var p : polygons) {
      if (!isSuperposedByBiggerPolygon.getOrDefault(p, false)) {
        res.add(p);
      }
    }
    return res;
  }

  private boolean isSuperposed(Polygon smallP, Polygon bigP, double maxAllowedIoU) {
    try {
      var inter = smallP.intersection(bigP);
      return inter.getArea() / smallP.getArea() > maxAllowedIoU;
    } catch (Exception e) {
      return false;
    }
  }
}
