package app.bpartners.geojobs.endpoint.rest.postprocessing;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import app.bpartners.geojobs.model.geometry.route.PrettyConf;
import app.bpartners.geojobs.model.geometry.route.UnionConf;
import org.junit.jupiter.api.Test;

import java.io.File;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon.toTiledPolygons;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tombe.TombeTest.invert;

class BoundaryMergerTest {
    private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
    PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/full-parcel.json");

    @Test
    void boundary_merge_on_tree() {
        var geojsonFile =
                new File(getClass().getResource("/ivandry/bati.geojson").getFile());

        var tilingConf = new TilingConf(20, 1_024);
        var unionConf = new UnionConf(0);
        var prettyConf = new PrettyConf(1);
        var mergeConf = new MergeConf(1, 2, 5);
        var boundaryMerger = new BoundaryMerger(tilingConf, unionConf, prettyConf, mergeConf, 10);

        var latLonPolygons = geoJsonLoader.apply(geojsonFile);
        var unified = boundaryMerger.apply(latLonPolygons);
        //var inverted = invert(unified);

        new Geojson(unified).saveAsFile("bati_v4.geojson");
    }


    @Test
    void boundary_merge_from_vgg() {
        var vgg = polygonProvider.getVggAnnotations();

        var tilingConf = new TilingConf(20, 1_024);
        var unionConf = new UnionConf(1);
        var prettyConf = new PrettyConf(1);
        var mergeConf = new MergeConf(1, 2, 5);
        var boundaryMerger = new BoundaryMerger(tilingConf, unionConf, prettyConf, mergeConf, 10);

        var tiledPolygons = toTiledPolygons(tilingConf, vgg, false);
        var unified = boundaryMerger.from(tiledPolygons);
        new Geojson(unified).saveAsFile("full_parcel.geojson");
    }

}
