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
                new File(getClass().getResource("/ivandry/arbres.geojson").getFile());

        var tilingConf = new TilingConf(20, 1_024);
        var unionConf = new UnionConf(5);
        var boundaryMerger = new BoundaryMerger(tilingConf, unionConf, 10);

        var latLonPolygons = geoJsonLoader.apply(geojsonFile);
        var unified = boundaryMerger.apply(latLonPolygons);
        var inverted = invert(unified);

        var expectedURI =
                Paths.get(getClass().getResource("/ivandry/arbre-merged.geojson").toURI());
        var expected = Files.readString(expectedURI);

        assertEquals(expected, new Geojson(inverted).stringValue());
    }

}
