package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.BATI_BETON;

import app.bpartners.geojobs.model.geometry.PolygonProvider;
import java.io.File;
import org.junit.jupiter.api.Test;

class BoundaryMergerTest {
  PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/dijon.json");
  BoundaryMerger boundaryMerger = new BoundaryMerger(4000, 41);

  @Test
  void run() {
    var geojsonFile = new File(getClass().getResource("/ivandry/bati.geojson").getFile());

    var unified = boundaryMerger.apply(geojsonFile, BATI_BETON);

    // new Geojson(unified).saveAsFile("MOISISSURE_NOIRCIE.geojson");
  }

  @Test
  void run_from_vgg() {
    var tiledPolygons = polygonProvider.getTiledPolygons(false);

    var unified = boundaryMerger.apply(tiledPolygons, BATI_BETON);

    // new Geojson(unified).saveAsFile("castanet-map-87-tree_initial.geojson");
  }
}
