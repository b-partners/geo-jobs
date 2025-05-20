package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.endpoint.rest.model.TileInfoSize;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

public class VGGFactoryTest {
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
}
