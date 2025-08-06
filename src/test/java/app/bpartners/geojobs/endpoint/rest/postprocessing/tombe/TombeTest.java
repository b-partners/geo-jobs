package app.bpartners.geojobs.endpoint.rest.postprocessing.tombe;

import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.MinimumBoundingRectangle;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.PolygonProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.invert;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.noSuperposition;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class TombeTest {
    PolygonProvider polygonProvider = new PolygonProvider("/geometry/vgg/vgg_annotations_notre_dame.json");
    GeoJsonLoader geoJsonLoader = new GeoJsonLoader();

    @Test
    void run() {
        var tiledPolygons = polygonProvider.getTiledPolygons(true);

        var actual = boundaryMerge(tiledPolygons);

        new Geojson(actual).saveAsFile("test_2.geojson");
    }

    @Test
    void run_from_geojson() {
        var geojsonFile = new File(getClass().getResource("/ivandry/concession_dame.geojson").getFile());
        var polygons = geoJsonLoader.apply(geojsonFile);
        var inverted = invert(polygons);

        var tiledPolygons = inverted.stream()
                .filter(ll -> !ll.polygon().isEmpty())
                .map(latLon -> latLon.tiledPolygon(TilingConf.getDefaultInstance()))
                .filter(ll -> ll.polygon().getArea() > 1000)
                .collect(toSet());

        var actual = boundaryMerge(tiledPolygons);

        new Geojson(actual).saveAsFile("concession_notre_dame.geojson");
    }

    private Set<LatLonPolygon> boundaryMerge(Set<TiledPolygon> polygons) {
        var rectangles = polygons.stream()
                .map(t -> {
                    var rect = new MinimumBoundingRectangle(t);
                    var width = rect.getWidth();
                    return rect.toEq()
                            .toMinimumBoundingRectangle(width, 90)
                            .toTiledPolygon();
                })
                .collect(toSet());

        var tmp = rectangles.stream()
                .map(TiledPolygon::latLonPolygon)
                .collect(toSet());
        return noSuperposition(tmp, 0.5);
    }
}
