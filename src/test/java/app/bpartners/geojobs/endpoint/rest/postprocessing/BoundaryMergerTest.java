package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.tombe.TombeTest.invert;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.BATI_BETON;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.model.geometry.LineInt;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

class BoundaryMergerTest {
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  TilingConf tilingConf = new TilingConf(20, 1_024);
  UnionConf unionConf = new UnionConf(10);
  PrettyConf prettyConf = new PrettyConf(5);
  MergeConf mergeConf = new MergeConf(1, 1, 2);
  BoundaryMerger boundaryMerger =
      new BoundaryMerger(tilingConf, unionConf, mergeConf, prettyConf, 10);

  @Test
  void run() {
    var geojsonFile =
        new File(
            getClass()
                .getResource("/ivandry/bati.geojson")
                .getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted = invert(polygons);

    var tiledPolygons = inverted.stream()
            .map(latLon -> latLon.tiledPolygon(tilingConf)).collect(Collectors.toSet());
    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    //new Geojson(unified).saveAsFile("bati_dijon.geojson");
  }

  @Test
  void is_collinear() {
    var line = new LineInt(new IntXY(10, 10), new IntXY(60, 60));
    var sameDirectionWithLine = new LineInt(new IntXY(0, 0), new IntXY(50, 50));
    var oppositeOfLine = new LineInt(new IntXY(60, 60), new IntXY(10, 10));
    var nonCollinearWithLine = new LineInt(new IntXY(10, 10), new IntXY(60, 50));
    var nullLine = new LineInt(new IntXY(10, 10), new IntXY(10, 10));
    var collinearWithLine = new LineInt(new IntXY(100, 100), new IntXY(150, 150));

    assertTrue(boundaryMerger.areVectorsCollinear(line, line));
    assertTrue(boundaryMerger.areVectorsCollinear(line, sameDirectionWithLine));
    assertTrue(boundaryMerger.areVectorsCollinear(line, oppositeOfLine));
    assertFalse(boundaryMerger.areVectorsCollinear(line, nonCollinearWithLine));
    assertTrue(boundaryMerger.areVectorsCollinear(line, nullLine));
    assertTrue(boundaryMerger.areVectorsCollinear(nullLine, nullLine));
    assertTrue(boundaryMerger.areVectorsCollinear(line, collinearWithLine));
  }

  @Test
  void is_collinear_enough() {
    var polyA = rectangle(0, 0, 10, 10);
    var polyB = rectangle(0, 20, 10, 30);
    var polyD = rectangle(15, 0, 25, 10);
    var polyE = rectangle(0, 0, 10, 10);
    var polyF =
        geometryFactory.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(10, 0),
              new Coordinate(5, 5),
              new Coordinate(0, 0)
            });
    var polyG =
        geometryFactory.createPolygon(
            new Coordinate[] {
              new Coordinate(0, 0),
              new Coordinate(5, 5),
              new Coordinate(10, 0),
              new Coordinate(0, 0)
            });
    var polyH = rectangle(0, 0, 10, 10);
    var polyI = rectangle(0, -20, 10, -10);

    assertTrue(boundaryMerger.isCollinearEnough(polyA, polyB));
    assertTrue(boundaryMerger.isCollinearEnough(polyA, polyD));
    assertTrue(boundaryMerger.isCollinearEnough(polyE, polyF));
    assertTrue(boundaryMerger.isCollinearEnough(polyG, polyG));
    assertTrue(boundaryMerger.isCollinearEnough(polyH, polyI));
  }

  @Test
  void find_linear_edges() {
    var polygon = rectangle(0, 0, 10, 10);

    assertEquals(4, boundaryMerger.findLinearEdges(polygon.getCoordinates()).size());
  }

  private Polygon rectangle(int x1, int y1, int x2, int y2) {
    return geometryFactory.createPolygon(
        new Coordinate[] {
          new Coordinate(x1, y1),
          new Coordinate(x2, y1),
          new Coordinate(x2, y2),
          new Coordinate(x1, y2),
          new Coordinate(x1, y1)
        });
  }
}
