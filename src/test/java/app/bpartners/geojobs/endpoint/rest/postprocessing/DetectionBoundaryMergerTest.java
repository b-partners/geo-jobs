package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.BoundaryMerger.invert;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.VGG;
import app.bpartners.geojobs.model.geometry.VGGFactory;
import app.bpartners.geojobs.service.GeometryPixelProjector;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.TileCoordinatesPolygonIntersection;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Slf4j
class DetectionBoundaryMergerTest {
  private final DetectionBoundaryMerger merger = new DetectionBoundaryMerger();
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  TilingConf tilingConf = new TilingConf(20, 1_024);
  GeometryPixelProjector geometryPixelProjector = new GeometryPixelProjector();
  GeometryConverter geometryConverter = new GeometryConverter(mock(), mock());
  TileCoordinatesPolygonIntersection tilePolygonIntersection =
      new TileCoordinatesPolygonIntersection(geometryPixelProjector, geometryConverter);
  GeometrySquareMeterArea geometrySquareMeterArea = new GeometrySquareMeterArea();
  VGGFactory vggFactory =
      new VGGFactory(
          tilePolygonIntersection, geometryConverter, geometrySquareMeterArea, objectMapper);

  @Test
  void apply_from_vgg() {
    var geojsonFile =
        new File(getClass().getResource("/geometry/geojson/bati_4_polygones.geojson").getFile());
    var latLonPolygons = geoJsonLoader.apply(geojsonFile);
    var invertedLatLonPolygons = invert(latLonPolygons);
    var polygons =
        invertedLatLonPolygons.stream()
            .map(
                latLon -> {
                  var polygon = latLon.polygon();

                  var metadata = new HashMap<String, Object>();
                  metadata.put("filename", "filename_1_2_3" + ".json");
                  metadata.put("label", "background");
                  metadata.put("confidence", 0.9);
                  polygon.setUserData(metadata);

                  return polygon;
                })
            .collect(toSet());

    var vgg = vggFactory.convert(polygons);

    Set<TiledPolygon> unified = merger.applyVgg(vgg);

    assertTrue(polygons.size() > unified.size());
    assertEquals(1, unified.size());
  }

  @Test
  void apply() {
    var geojsonFile =
        new File(getClass().getResource("/geometry/geojson/bati_4_polygones.geojson").getFile());
    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted = invert(polygons);

    Set<LatLonPolygon> unified = merger.apply(toTiledPolygon(inverted));

    assertTrue(polygons.size() > unified.size());
    assertEquals(1, unified.size());
  }

  @Test
  @Disabled("Local use only: Visualisation test")
  void run_from_vgg_list() throws Exception {
    InputStream is = getClass().getResourceAsStream("/vgg/polygon-1.json");
    List<VGG> vggList = objectMapper.readValue(is, new TypeReference<List<VGG>>() {});
    VGG vgg = vggList.get(0);

    var unified = merger.applyVgg(vgg).stream().map(TiledPolygon::latLonPolygon).collect(toSet());

    // new Geojson(unified).saveAsFile("unified_polygon-1.geojson");
  }

  @Test
  @Disabled("Local use only: Visualisation test")
  void merge_bati_4_polygones() {
    var geojsonFile =
        new File(getClass().getResource("/geometry/geojson/bati_4_polygones.geojson").getFile());
    var expectedMergedGeoJsonFiel =
        new File(
            getClass().getResource("/geometry/geojson/bati_4_polygones_merged.geojson").getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var expectedMergedPolygons = geoJsonLoader.apply(expectedMergedGeoJsonFiel);
    var inverted = invert(polygons);

    var tiledPolygons = toTiledPolygon(inverted);
    var unified = merger.apply(tiledPolygons);

    // new Geojson(unified).saveAsFile("bati_4_polygones_merged.geojson");
  }

  private Set<TiledPolygon> toTiledPolygon(Set<LatLonPolygon> polygons) {
    return polygons.stream().map(latLon -> latLon.tiledPolygon(tilingConf)).collect(toSet());
  }
}
