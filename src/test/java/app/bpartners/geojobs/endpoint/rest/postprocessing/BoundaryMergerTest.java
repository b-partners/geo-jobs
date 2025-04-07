package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.tombe.TombeTest.invert;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundaryMergerTest {
    private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();

    @Test
    void boundary_merge_on_tree() throws IOException, URISyntaxException {
        var geojsonFile =
                new File(getClass().getResource("/ivandry/Arbres_lens.geojson").getFile());

        var tilingConf = new TilingConf(20, 1_024);
        var unionConf = new UnionConf(1);
        var boundaryMerger = new BoundaryMerger(tilingConf, unionConf, 10);

        var latLonPolygons = geoJsonLoader.apply(geojsonFile);
        var unified = boundaryMerger.apply(latLonPolygons);

        new Geojson(unified).saveAsFile("Arbres_lens_v2.geojson");
    }

}
