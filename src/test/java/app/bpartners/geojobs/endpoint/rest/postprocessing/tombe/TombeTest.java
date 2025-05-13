package app.bpartners.geojobs.endpoint.rest.postprocessing.tombe;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.model.geometry.area.Area;
import app.bpartners.geojobs.model.geometry.area.SquareDegree;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

@Slf4j
public class TombeTest {

  @Test
  void postprocess_tombes() throws IOException, URISyntaxException {
    var geojson =
        new Geojson(new File(getClass().getResource("/geometry/tombes.geojson").getFile()));
    var polygons = invert(geojson.polygons());

    var m2toDeg2 = 1E-11; // France
    var tombeMinArea = new SquareDegree(112 * m2toDeg2);
    var filteredByMinAreaPolygons = filterByMinArea(polygons, tombeMinArea);
    assertEquals(2528, polygons.size());
    assertEquals(2449, filteredByMinAreaPolygons.size());

    var maxAllowedIoU = 0.2;
    var noSuperpositionPolygons = noSuperposition(filteredByMinAreaPolygons, maxAllowedIoU);
    assertEquals(2195, noSuperpositionPolygons.size());

    var postprocessedPolygons = noSuperpositionPolygons;
    var expectedURI =
        Paths.get(getClass().getResource("/geometry/tombes-postprocessed.geojson").toURI());
    var expected = Files.readString(expectedURI);

    assertEquals(expected, new Geojson(postprocessedPolygons).stringValue());
  }

  public static Set<LatLonPolygon> invert(Set<LatLonPolygon> noSuperpositionPolygons) {
    return noSuperpositionPolygons.stream()
        .map(
            p -> {
              var polygon =
                  geometryFactory.createPolygon(
                      Arrays.stream(p.polygon().getCoordinates())
                          .map(c -> new Coordinate(c.y, c.x))
                          .toArray(Coordinate[]::new));
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
