package app.bpartners.geojobs.endpoint.rest.postprocessing.tombe;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.withOffset;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.originTile;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.area.Area;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import app.bpartners.geojobs.model.geometry.route.UnifiedRoute;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class TombeTest {
  private final TilingConf tilingConf = new TilingConf(20, 1024);
  private final UnionConf unionConf = new UnionConf(0);

  @Test
  void postprocess_tombes() throws IOException, URISyntaxException {
    var geojson =
        new Geojson(new File(getClass().getResource("/ivandry/annotations_rectangles.json.geojson").getFile()));
    var polygons = invert(geojson.polygons());

    var m2toDeg2 = 1E-11; // France
    var tombeMinArea = new SquareDegree(112 * m2toDeg2);
    //var filteredByMinAreaPolygons = filterByMinArea(polygons, tombeMinArea);
    // assertEquals(2528, polygons.size());
    // assertEquals(2449, filteredByMinAreaPolygons.size());

    var maxAllowedIoU = 0.2;
    var noSuperpositionPolygons = noSuperposition(polygons, maxAllowedIoU);
    // assertEquals(2195, noSuperpositionPolygons.size());

    var postprocessedPolygons = merge(noSuperpositionPolygons, 2000);
    var expectedURI =
        Paths.get(getClass().getResource("/geometry/tombes-postprocessed.geojson").toURI());
    var expected = Files.readString(expectedURI);

    new Geojson(postprocessedPolygons).saveAsFile("annotation.geojson");
    //assertEquals(expected, new Geojson(postprocessedPolygons).stringValue());
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

  private Set<LatLonPolygon> merge(Set<LatLonPolygon> polygons, int minArea) {
    var origin = originTile(new ArrayList<>(polygons).getFirst().polygon().getCoordinate(), tilingConf.z());
    var tiledPolygonsWithOffset = polygons.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).toList();
    var result = new HashSet<TiledPolygon>();
    for (var tp : tiledPolygonsWithOffset) {
      var aroundPolygons = tiledPolygonsWithOffset.stream().filter(p -> smallAround(tp, p, minArea)).collect(toSet());
      if (!aroundPolygons.isEmpty()) {
        var smallest = aroundPolygons.stream()
                .map(TiledPolygon::polygon)
                .sorted(Comparator.comparing(Polygon::getArea))
                .toList().getFirst();
        var unified = new UnifiedRoute(Set.of(smallest, tp.polygon()), unionConf).unified();
        for (var p : unified) {
          result.add(
                  new TiledPolygon(p, tp.type(), tp.originTile(), tp.tilingConf()));
        }
      }
    }
     return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(toSet());
  }

  private boolean smallAround(TiledPolygon ref, TiledPolygon other, int minArea) {
    if (other.polygon().getArea() > minArea) {
      return false;
    }

    if (ref.originTile().equals(other.originTile())) {
      return false;
    }

    if (ref.polygon().distance(other.polygon()) > 10) {
      return false;
    }

    var bufferedRef = ref.polygon().getBoundary().buffer(0.5);
    var boundaryOther = other.polygon().getBoundary();
    var intersection = bufferedRef.intersection(boundaryOther);

    return intersection.getLength() >= 10;
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
