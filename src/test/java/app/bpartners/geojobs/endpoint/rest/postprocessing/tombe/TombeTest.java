package app.bpartners.geojobs.endpoint.rest.postprocessing.tombe;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.withOffset;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
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
  PolygonProvider polygonProvider = new PolygonProvider("/ivandry/vgg_annotations_modified.json");
  private final TilingConf tilingConf = new TilingConf(20, 1024);
  private final UnionConf unionConf = new UnionConf(5);

  @Test
  void postprocess_tombes() throws IOException, URISyntaxException {
    var geojson =
        new Geojson(new File(getClass().getResource("/ivandry/annotation.geojson").getFile()));
    var polygons = invert(geojson.polygons());

    var m2toDeg2 = 1E-11; // France
    var tombeMinArea = new SquareDegree(112 * m2toDeg2);
    var filteredByMinAreaPolygons = filterByMinArea(polygons, tombeMinArea);
    // assertEquals(2528, polygons.size());
    // assertEquals(2449, filteredByMinAreaPolygons.size());

    var maxAllowedIoU = 0.6;
    var noSuperpositionPolygons = noSuperposition(filteredByMinAreaPolygons, maxAllowedIoU);
    // assertEquals(2195, noSuperpositionPolygons.size());

    var postprocessedPolygons = noSuperpositionPolygons;
    var expectedURI =
        Paths.get(getClass().getResource("/geometry/tombes-postprocessed.geojson").toURI());
    var expected = Files.readString(expectedURI);

    new Geojson(postprocessedPolygons).saveAsFile("annotation_postprocessed.geojson");
    //assertEquals(expected, new Geojson(postprocessedPolygons).stringValue());
  }

  @Test
  void boundary_merge(){
    var tiledPolygons = polygonProvider.getTiledPolygons(true);

    var postProcessedPolygons = merge(tiledPolygons, 4000);

    new Geojson(postProcessedPolygons).saveAsFile("annotation.geojson");
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

  private Set<LatLonPolygon> merge(Set<TiledPolygon> polygons, int minArea) {
    var origin = new ArrayList<>(polygons).getFirst().originTile();
    var tiledPolygonsWithOffset = polygons.stream().map(tile -> withOffset(tile, origin, tilingConf)).toList();
    var result = new HashSet<TiledPolygon>();
    var alreadyUnified = new HashSet<Polygon>();
    for (var tp : tiledPolygonsWithOffset) {
      if (alreadyUnified.contains(tp.polygon())) {
        continue;
      }
      var aroundPolygons = tiledPolygonsWithOffset.stream().filter(p -> smallestAround(tp, p, minArea)).collect(toSet());
      if (!aroundPolygons.isEmpty()) {
        var smallest = new ArrayList<>(aroundPolygons.stream()
                .map(TiledPolygon::polygon)
                .toList());
        smallest.sort(Comparator.comparing(Polygon::getArea));
        var toUnify = smallest.getFirst();
        alreadyUnified.addAll(Set.of(toUnify, tp.polygon()));
        var unified = new UnifiedRoute(Set.of(toUnify, tp.polygon()), unionConf).unified();
        for (var p : unified) {
          result.add(
                  new TiledPolygon((Polygon) p.getEnvelope(), tp.type(), tp.originTile(), tp.tilingConf()));
        }
      }else {
        result.add(tp);
      }
    }
    log.info("Already unified polygons: {}", alreadyUnified.size());
    return result.stream().map(tp -> tp.latLonPolygon(origin)).collect(toSet());
  }

  private boolean smallestAround(TiledPolygon ref, TiledPolygon other, int minArea) {
    var origin  = ref.originTile();
    var tile = other.originTile();
    if (other.polygon().getArea() > minArea) {
      return false;
    }

    if (ref.originTile().equals(other.originTile())) {
      return false;
    }
    int dx = Math.abs(tile.x() - origin.x());
    int dy = Math.abs(tile.y() - origin.y());
    var refBoundary = ref.polygon().getBoundary().buffer(10);
    var otherBoundary = other.polygon().getBoundary().buffer(10);
    var intersection = refBoundary.intersection(otherBoundary);
    return (-1 <= dx && dy <= 1) && intersection.getLength() > 100;
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
