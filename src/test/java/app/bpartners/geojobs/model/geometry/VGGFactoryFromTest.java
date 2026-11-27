package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.postprocessing.DetectionBoundaryMerger;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.GeometryPixelProjector;
import app.bpartners.geojobs.service.PolygonCoordinatesCloser;
import app.bpartners.geojobs.service.TileCoordinatesPolygonIntersection;
import app.bpartners.geojobs.service.TiledPixelPolygonComputer;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class VGGFactoryFromTest {
  private static final int TILE_X = 544_680;
  private static final int TILE_Y = 383_095;
  private static final int ZOOM = 20;
  private static final int TILE_SIZE = 1_024;

  GeometryConverter geometryConverter = new GeometryConverter();
  TileCoordinatesPolygonIntersection intersection =
      new TileCoordinatesPolygonIntersection(new GeometryPixelProjector(), geometryConverter);
  TiledPixelPolygonComputer computer =
      new TiledPixelPolygonComputer(
          geometryConverter, intersection, new PolygonCoordinatesCloser());
  VGGFactory subject =
      new VGGFactory(intersection, geometryConverter, new DetectionBoundaryMerger());

  private static List<BigDecimal> lonLat(double lon, double lat) {
    return List.of(BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
  }

  private static Feature polygonFeature(
      double minLon, double maxLon, Map<String, Object> properties) {
    var minLat = 43.5504;
    var maxLat = 43.5509;
    var ring =
        List.of(
            lonLat(minLon, minLat),
            lonLat(maxLon, minLat),
            lonLat(maxLon, maxLat),
            lonLat(minLon, maxLat),
            lonLat(minLon, minLat));
    return new Feature()
        .properties(properties)
        .geometry(
            new FeatureGeometry(
                new Polygon().type(Polygon.TypeEnum.POLYGON).coordinates(List.of(ring))));
  }

  private static DetectedObject pixelSquare(int minPx, int maxPx) {
    var geometry =
        "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[%d,%d],[%d,%d],[%d,%d],[%d,%d]]]]}"
            .formatted(minPx, minPx, maxPx, minPx, maxPx, maxPx, minPx, maxPx);
    var feature =
        app.bpartners.geojobs.repository.model.Feature.builder()
            .geometry(
                app.bpartners.geojobs.repository.model.Feature.FeatureGeometry.builder()
                    .geometryType(MULTI_POLYGON)
                    .actualInstanceStringValue(geometry)
                    .build())
            .build();
    return DetectedObject.builder()
        .feature(feature)
        .detectedObjectType(DetectableObjectType.builder().detectableType(PISCINE).build())
        .build();
  }

  private static MachineDetectedTile tile(int x, DetectedObject object) {
    return MachineDetectedTile.builder()
        .tile(Tile.builder().coordinates(new TileCoordinates().x(x).y(TILE_Y).z(ZOOM)).build())
        .detectedObjects(List.of(object))
        .build();
  }

  private List<TiledPixelPolygon> tiledPixelPolygons(
      Feature zone, Feature delimitation, MachineDetectedTile... tiles) {
    return computer.apply(zone, List.of(delimitation), List.of(PISCINE), List.of(tiles), false);
  }

  private static List<String> labels(VGG.Annotation annotation) {
    return annotation.getRegions().values().stream()
        .map(region -> (String) region.getRegionAttribute().get("label"))
        .sorted()
        .toList();
  }

  @Test
  void builds_one_annotation_per_feature_with_detected_objects_and_roof_regions() {
    var zone = polygonFeature(7.0009, 7.0015, new HashMap<>());
    var delimitation = polygonFeature(7.0009, 7.0015, new HashMap<>());
    var polygons = tiledPixelPolygons(zone, delimitation, tile(TILE_X, pixelSquare(100, 300)));

    var actual = subject.from(polygons, List.of(new TileCoordinates().x(TILE_X).y(TILE_Y).z(ZOOM)));

    assertEquals(1, actual.size());
    var vgg = actual.get(delimitation);
    assertEquals(1, vgg.size());
    var annotation = vgg.values().iterator().next();
    assertTrue(annotation.getFilename().endsWith("_20_" + TILE_X + "_" + TILE_Y + ".png"));
    assertEquals(List.of("PISCINE", "TOITURE_REVETEMENT"), labels(annotation));
    var shape =
        annotation.getRegions().values().stream()
            .filter(region -> "PISCINE".equals(region.getRegionAttribute().get("label")))
            .findFirst()
            .orElseThrow()
            .getShapeAttribute();
    assertEquals("Polygon", shape.getName());
    assertEquals(100.0, shape.getAllPointsX().stream().min(Double::compare).orElseThrow(), 1.0);
    assertEquals(300.0, shape.getAllPointsX().stream().max(Double::compare).orElseThrow(), 1.0);
  }

  @Test
  void keeps_feature_properties_except_identifiers() {
    var properties = new HashMap<String, Object>();
    properties.put("id", "feature-id");
    properties.put("feature_id", "other-id");
    properties.put("address", "1 rue de Paris");
    var zone = polygonFeature(7.0009, 7.0015, new HashMap<>());
    var delimitation = polygonFeature(7.0009, 7.0015, properties);
    var polygons = tiledPixelPolygons(zone, delimitation, tile(TILE_X, pixelSquare(100, 300)));

    var actual = subject.from(polygons, List.of(new TileCoordinates().x(TILE_X).y(TILE_Y).z(ZOOM)));

    var annotationProperties = actual.get(delimitation).values().iterator().next().getProperties();
    assertEquals(Map.of("address", "1 rue de Paris"), annotationProperties);
  }

  @Test
  void projects_polygons_of_each_tile_on_the_composite_image_and_merges_the_roof() {
    var zone = polygonFeature(7.0009, 7.0017, new HashMap<>());
    var delimitation = polygonFeature(7.0009, 7.0016, new HashMap<>());
    var polygons =
        tiledPixelPolygons(
            zone,
            delimitation,
            tile(TILE_X, pixelSquare(100, 300)),
            tile(TILE_X + 1, pixelSquare(100, 300)));
    var envelop =
        List.of(
            new TileCoordinates().x(TILE_X).y(TILE_Y).z(ZOOM),
            new TileCoordinates().x(TILE_X + 1).y(TILE_Y).z(ZOOM));

    var actual = subject.from(polygons, envelop);

    var annotation = actual.get(delimitation).values().iterator().next();
    var xCoordinates =
        annotation.getRegions().values().stream()
            .flatMap(region -> region.getShapeAttribute().getAllPointsX().stream())
            .toList();
    assertTrue(xCoordinates.stream().anyMatch(x -> x > 2 * TILE_SIZE - 600));
    assertTrue(xCoordinates.stream().anyMatch(x -> x >= TILE_SIZE + 100 - 1));
    assertTrue(xCoordinates.stream().allMatch(x -> x <= 2 * TILE_SIZE + 1));
    assertEquals(List.of("PISCINE", "PISCINE", "TOITURE_REVETEMENT"), labels(annotation));
  }

  @Test
  void returns_nothing_for_nothing_to_project() {
    var actual = subject.from(List.of(), List.of(new TileCoordinates().x(TILE_X).y(TILE_Y)));

    assertTrue(actual.isEmpty());
  }

  @Test
  void rejects_an_empty_envelop() {
    var noEnvelop = Collections.<TileCoordinates>emptyList();
    var noPolygons = Collections.<TiledPixelPolygon>emptyList();

    assertThrows(NoSuchElementException.class, () -> subject.from(noPolygons, noEnvelop));
  }

  @Test
  void annotation_filename_embeds_zoom_and_minimum_tile_of_the_envelop() {
    var zone = polygonFeature(7.0009, 7.0015, new HashMap<>());
    var delimitation = polygonFeature(7.0009, 7.0015, new HashMap<>());
    var polygons = tiledPixelPolygons(zone, delimitation, tile(TILE_X, pixelSquare(100, 300)));
    var envelop =
        List.of(
            new TileCoordinates().x(TILE_X + 5).y(TILE_Y + 7).z(ZOOM),
            new TileCoordinates().x(TILE_X).y(TILE_Y).z(ZOOM));

    var actual = subject.from(polygons, envelop);

    var filename = actual.get(delimitation).values().iterator().next().getFilename();
    assertTrue(filename.endsWith("_20_" + TILE_X + "_" + TILE_Y + ".png"));
    assertFalse(filename.isBlank());
  }
}
