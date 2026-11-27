package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class TiledPixelPolygonComputerTest {
  private static final int TILE_X = 544_680;
  private static final int TILE_Y = 383_095;
  private static final int ZOOM = 20;
  private static final double PIXEL_AREA_TOLERANCE = 1.0;

  GeometryConverter geometryConverter = new GeometryConverter();
  TiledPixelPolygonComputer subject =
      new TiledPixelPolygonComputer(
          geometryConverter,
          new TileCoordinatesPolygonIntersection(new GeometryPixelProjector(), geometryConverter),
          new PolygonCoordinatesCloser());

  private static List<BigDecimal> lonLat(double lon, double lat) {
    return List.of(BigDecimal.valueOf(lon), BigDecimal.valueOf(lat));
  }

  private static Feature polygonFeature(
      double minLon, double minLat, double maxLon, double maxLat) {
    var ring =
        List.of(
            lonLat(minLon, minLat),
            lonLat(maxLon, minLat),
            lonLat(maxLon, maxLat),
            lonLat(minLon, maxLat),
            lonLat(minLon, minLat));
    return new Feature()
        .geometry(
            new FeatureGeometry(
                new Polygon().type(Polygon.TypeEnum.POLYGON).coordinates(List.of(ring))));
  }

  private static Feature zoneCoveringTile() {
    return polygonFeature(7.0009, 43.5504, 7.0015, 43.5509);
  }

  private static Feature delimitationCoveringTheLeftOfTile() {
    return polygonFeature(7.0009, 43.5504, 7.0012, 43.5509);
  }

  private static DetectedObject pixelSquare(DetectableType type, int minPx, int maxPx) {
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
        .detectedObjectType(DetectableObjectType.builder().detectableType(type).build())
        .build();
  }

  private static MachineDetectedTile tile(int x, int y, DetectedObject... objects) {
    return MachineDetectedTile.builder()
        .tile(Tile.builder().coordinates(new TileCoordinates().x(x).y(y).z(ZOOM)).build())
        .detectedObjects(List.of(objects))
        .build();
  }

  @Test
  void keeps_detected_object_polygons_inside_the_delimitation_with_tile_coordinates() {
    var delimitation = delimitationCoveringTheLeftOfTile();

    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitation),
            List.of(TOITURE_REVETEMENT),
            List.of(tile(TILE_X, TILE_Y, pixelSquare(TOITURE_REVETEMENT, 100, 300))),
            false);

    assertEquals(1, actual.size());
    TiledPixelPolygon tiledPixelPolygon = actual.getFirst();
    assertEquals(delimitation, tiledPixelPolygon.feature());
    assertEquals(TILE_X, tiledPixelPolygon.tileX());
    assertEquals(TILE_Y, tiledPixelPolygon.tileY());
    assertEquals(ZOOM, tiledPixelPolygon.zoom());
    assertEquals(1, tiledPixelPolygon.polygons().size());
    var polygonObjectType = tiledPixelPolygon.polygons().getFirst();
    assertEquals(TOITURE_REVETEMENT, polygonObjectType.objectType());
    assertEquals(40_000.0, polygonObjectType.polygon().getArea(), PIXEL_AREA_TOLERANCE);
  }

  @Test
  void drops_detected_objects_lying_outside_the_delimitation() {
    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitationCoveringTheLeftOfTile()),
            List.of(TOITURE_REVETEMENT),
            List.of(tile(TILE_X, TILE_Y, pixelSquare(TOITURE_REVETEMENT, 800, 900))),
            false);

    assertEquals(1, actual.size());
    assertTrue(actual.getFirst().polygons().isEmpty());
  }

  @Test
  void ignores_detected_objects_of_types_that_are_not_requested() {
    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitationCoveringTheLeftOfTile()),
            List.of(TOITURE_REVETEMENT),
            List.of(tile(TILE_X, TILE_Y, pixelSquare(PISCINE, 100, 300))),
            false);

    assertEquals(1, actual.size());
    assertTrue(actual.getFirst().polygons().isEmpty());
  }

  @Test
  void keeps_each_requested_type_of_a_tile_with_several_detected_objects() {
    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitationCoveringTheLeftOfTile()),
            List.of(TOITURE_REVETEMENT, PISCINE),
            List.of(
                tile(
                    TILE_X,
                    TILE_Y,
                    pixelSquare(TOITURE_REVETEMENT, 100, 200),
                    pixelSquare(PISCINE, 300, 400))),
            false);

    var types = actual.getFirst().polygons().stream().map(p -> p.objectType()).toList();
    assertEquals(List.of(TOITURE_REVETEMENT, PISCINE), types);
  }

  @Test
  void skips_tiles_that_do_not_intersect_the_provided_zone() {
    var farTile = tile(TILE_X + 50, TILE_Y, pixelSquare(TOITURE_REVETEMENT, 100, 300));

    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitationCoveringTheLeftOfTile()),
            List.of(TOITURE_REVETEMENT),
            List.of(farTile),
            false);

    assertTrue(actual.isEmpty());
  }

  @Test
  void produces_one_result_per_delimitation_and_intersecting_tile() {
    var firstDelimitation = delimitationCoveringTheLeftOfTile();
    var secondDelimitation = polygonFeature(7.0012, 43.5504, 7.0015, 43.5509);

    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(firstDelimitation, secondDelimitation),
            List.of(TOITURE_REVETEMENT),
            List.of(tile(TILE_X, TILE_Y, pixelSquare(TOITURE_REVETEMENT, 100, 300))),
            false);

    assertEquals(2, actual.size());
    assertEquals(firstDelimitation, actual.getFirst().feature());
    assertEquals(secondDelimitation, actual.getLast().feature());
    assertNotNull(actual.getLast().polygons());
    assertTrue(actual.getLast().polygons().isEmpty());
  }

  @Test
  void skips_object_types_without_any_processed_geometry() {
    var delimitationWithoutGeometry = new Feature();

    var actual =
        subject.apply(
            zoneCoveringTile(),
            List.of(delimitationWithoutGeometry),
            List.of(TOITURE_REVETEMENT),
            List.of(tile(TILE_X, TILE_Y, pixelSquare(TOITURE_REVETEMENT, 100, 300))),
            false);

    assertEquals(1, actual.size());
    assertTrue(actual.getFirst().polygons().isEmpty());
  }
}
