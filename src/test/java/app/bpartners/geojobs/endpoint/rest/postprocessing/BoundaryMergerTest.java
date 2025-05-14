package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class BoundaryMergerTest {
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();

  @Test
  void boundary_merge_on_bati() throws IOException, URISyntaxException {
    var geojsonFile = new File(getClass().getResource("/ivandry/bati.geojson").getFile());

    var tilingConf = new TilingConf(20, 1_024);
    var unionConf = new UnionConf(10);
    var prettyConf = new PrettyConf(1);
    var mergeConf = new MergeConf(2, 2, 5);
    var boundaryMerger = new BoundaryMerger(tilingConf, unionConf, prettyConf, mergeConf, 41);

    var latLonPolygons = geoJsonLoader.apply(geojsonFile);
    var unified = boundaryMerger.apply(latLonPolygons);

    var expectedURI = Paths.get(getClass().getResource("/ivandry/bati_merged.geojson").toURI());
    var expected = Files.readString(expectedURI);

    // new
    // Geojson(invert(unified)).saveAsFile("map95_v2_0.05_fusion_cimetiere_vgg_annotations_merged.geojson");
    assertEquals(expected, new Geojson(unified).stringValue());
  }
}
