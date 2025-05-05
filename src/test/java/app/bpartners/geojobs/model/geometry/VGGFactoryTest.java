package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.endpoint.rest.model.Geometry.TypeEnum.MULTI_POLYGON;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.HUMIDITE;
import static app.bpartners.geojobs.service.event.ParcelParcelParcelTilingTaskCreatedServiceIT.defaultFeature;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectType;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

public class VGGFactoryTest {
  private final PolygonProvider polygonProvider =
      new PolygonProvider("/geometry/vgg/pathway.json", null, new IntXY(1024, 1024));
  private final FeatureMapper featureMapper = new FeatureMapper();
  private final VGGFactory subject = new VGGFactory(featureMapper);

  @Test
  void features_to_vgg_ok() {
    var features = polygonProvider.getPolygons();
    var expectedFilename = "5cm3346073745629231615_20_538860_367572.jpg";

    var actual = subject.convert(features);

    assertEquals(5, actual.size());
    assertEquals(2, actual.get(expectedFilename).getRegions().size());
  }

  @Test
  void detected_tiles_to_vgg_ok() throws IOException {
    var expectedFilename = "20_538860_367571.jpg";

    var actual = subject.from(List.of(detectedTile()));

    assertEquals(1, actual.size());
    assertEquals(1, actual.get(expectedFilename).getRegions().size());
  }

  DetectedTile detectedTile() {
    return DetectedTile.builder()
        .tile(Tile.builder().coordinates(new TileCoordinates().x(538860).y(367571).z(20)).build())
        .detectedObjects(
            List.of(
                DetectedObject.builder()
                    .feature(
                        Feature.builder()
                            .geometry(
                                Feature.FeatureGeometry.builder()
                                    .geometryType(MULTI_POLYGON)
                                    .actualInstanceStringValue(
                                        defaultFeature()
                                            .getGeometry()
                                            .getActualInstanceStringValue())
                                    .build())
                            .build())
                    .detectedObjectType(
                        DetectableObjectType.builder().detectableType(HUMIDITE).build())
                    .build()))
        .build();
  }
}
