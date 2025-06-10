package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.tombe.TombeTest.invert;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.BATI_BETON;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BoundaryMergerTest {
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/dijon.json");
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  TilingConf tilingConf = new TilingConf(20, 1_024);
  UnionConf unionConf = new UnionConf(5);
  PrettyConf prettyConf = new PrettyConf(1);
  MergeConf mergeConf = new MergeConf(0, 0);
  BoundaryMerger boundaryMerger =
      new BoundaryMerger(tilingConf, unionConf, mergeConf, prettyConf, 41);

  @Test
  void run() {
    var geojsonFile = new File(getClass().getResource("/ivandry/bati.geojson").getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted = invert(polygons);

    var tiledPolygons =
        inverted.stream()
            .map(latLon -> latLon.tiledPolygon(tilingConf))
            .collect(Collectors.toSet());
    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    // new Geojson(unified).saveAsFile("bati_dijon.geojson");
  }

  @Test
  void run_from_vgg() {
    var tiledPolygons = polygonProvider.getTiledPolygons(false);

    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    // new Geojson(unified).saveAsFile("castanet-map-87-tree_initial.geojson");
  }
}
