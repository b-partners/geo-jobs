package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.tombe.TombeTest.invert;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonLoader;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

class VGGFactoryTest {
  private final GeoJsonLoader geoJsonLoader = new GeoJsonLoader();
  private final PolygonProvider polygonProvider =
      new PolygonProvider("/geometry/vgg/pathway.json", null, new IntXY(1024, 1024));
  private final FeatureMapper featureMapper = new FeatureMapper(new GeometryConverter(null));
  private final VGGFactory subject = new VGGFactory(featureMapper);

  public static DetectedTile detectedTile() {
    String humiditeGeometry =
        """
        {
          "type": "MultiPolygon",
          "coordinates": [ [ [
            [ 100.0, 200.0 ],
            [ 150.0, 210.0 ],
            [ 160.0, 180.0 ],
            [ 120.0, 170.0 ],
            [ 100.0, 200.0 ]
          ] ] ]
        }
        """;
    String usureGeometry =
        """
        {
          "type": "MultiPolygon",
          "coordinates": [ [ [
            [ 50.0, 50.0 ],
            [ 70.0, 60.0 ],
            [ 60.0, 80.0 ],
            [ 40.0, 70.0 ],
            [ 50.0, 50.0 ]
          ] ] ]
        }

        """;

    String moisissure =
        """
        {
          "type": "MultiPolygon",
          "coordinates": [ [ [
            [ 200.0, 100.0 ],
            [ 220.0, 110.0 ],
            [ 210.0, 130.0 ],
            [ 190.0, 120.0 ],
            [ 200.0, 100.0 ]
          ] ] ]
        }

        """;

    Feature feature = Feature.builder().id("feature").zoom(20).build();

    return DetectedTile.builder()
        .tile(
            Tile.builder()
                .coordinates(new TileCoordinates().x(0).y(0).z(20))
                .size(new TileInfoSize().height(1024).width(1024))
                .build())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .feature(
                        feature.toBuilder()
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(humiditeGeometry)
                                    .build())
                            .build())
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(HUMIDITE_CLAIR).build())
                    .build(),
                DetectedObject.builder()
                    .feature(
                        feature.toBuilder()
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(usureGeometry)
                                    .build())
                            .build())
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(USURE_LEGER).build())
                    .build(),
                DetectedObject.builder()
                    .feature(
                        feature.toBuilder()
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(moisissure)
                                    .build())
                            .build())
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(MOISISSURE_COULEUR).build())
                    .build()))
        .build();
  }

  @Test
  void features_to_vgg_ok() {
    var features = polygonProvider.getPolygons();
    var expectedFilename = "5cm3346073745629231615_20_538860_367572.jpg";

    var actual = subject.convert(features);

    assertEquals(5, actual.size());
    assertEquals(2, actual.get(expectedFilename).getRegions().size());
  }

  @Test
  void geojson_to_vgg() throws IOException {
    var geojsonFile = new File(getClass().getResource("/ivandry/bati.geojson").getFile());

    var polygons = geoJsonLoader.apply(geojsonFile);
    var inverted =
        invert(polygons).stream()
            .map(latLonPolygon -> latLonPolygon.tiledPolygon(new TilingConf(20, 1024)))
            .collect(Collectors.toSet());
    var actual = subject.from(inverted);

    // Files.write(new File("bati.json").toPath(), actual.getBytes());
  }

  @Test
  void detected_tiles_to_vgg_ok() {
    Coordinate[] boundingCoords =
        new Coordinate[] {
          new Coordinate(465.95744680851067, 282.97872340425533),
          new Coordinate(780.8510638297872, 421.2765957446809),
          new Coordinate(619.1489361702128, 800.0),
          new Coordinate(474.468085106383, 729.7872340425532),
          new Coordinate(510.63829787234044, 636.1702127659574),
          new Coordinate(351.06382978723406, 557.4468085106383),
          new Coordinate(465.95744680851067, 282.97872340425533)
        };

    LinearRing shell = geometryFactory.createLinearRing(boundingCoords);
    Polygon roofGeometry = geometryFactory.createPolygon(shell, null);

    var actual = subject.from(roofGeometry, List.of(detectedTile()));

    var filename = actual.keySet().stream().toList().getFirst();
    assertEquals(1, actual.size());
    assertEquals(3, actual.get(filename).getRegions().size());
  }

  @Test
  void transform_list_polygons_into_map_ok() {
    Coordinate[] coordinates =
        new Coordinate[] {
          new Coordinate(1, 2), new Coordinate(3, 4), new Coordinate(5, 6), new Coordinate(1, 2)
        };
    Polygon polygon = geometryFactory.createPolygon(coordinates);
    PolygonObjectType polygonObjectType = new PolygonObjectType(polygon, DetectableType.TROTTOIR);
    app.bpartners.geojobs.endpoint.rest.model.Feature feature =
        new app.bpartners.geojobs.endpoint.rest.model.Feature()
            .type(app.bpartners.geojobs.endpoint.rest.model.Feature.TypeEnum.FEATURE)
            .geometry(
                new FeatureGeometry(
                    new Point()
                        .type(Point.TypeEnum.POINT)
                        .coordinates(List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2)))))
            .properties(new HashMap<>(Map.of("id", "feature-1", "zoom", 20)));
    TiledPixelPolygon tiledPixelPolygon =
        new TiledPixelPolygon(feature, List.of(polygonObjectType), 10, 20, 20);

    List<TiledPixelPolygon> inputTiledPixelPolygons = List.of(tiledPixelPolygon);
    MultiPolygon roofLatLonMultiPolygonMock = mock(MultiPolygon.class);
    when(roofLatLonMultiPolygonMock.getArea()).thenReturn(1000.0);
    when(roofLatLonMultiPolygonMock.getNumGeometries()).thenReturn(1);
    when(roofLatLonMultiPolygonMock.getGeometryN(0)).thenReturn(polygon);

    Map<app.bpartners.geojobs.endpoint.rest.model.Feature, VGG> result =
        subject.from(inputTiledPixelPolygons, roofLatLonMultiPolygonMock);

    Assertions.assertNotNull(result);
    assertEquals(1, result.size());
    Assertions.assertTrue(result.containsKey(feature));
    VGG actualVgg = result.get(feature);
    Assertions.assertNotNull(actualVgg);
  }
}
