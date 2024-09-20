package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.PISCINE;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.HUMAN;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.gen.annotator.endpoint.rest.model.Annotation;
import app.bpartners.gen.annotator.endpoint.rest.model.Label;
import app.bpartners.gen.annotator.endpoint.rest.model.Point;
import app.bpartners.gen.annotator.endpoint.rest.model.Polygon;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionResponse;
import app.bpartners.geojobs.service.tiling.TileValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DetectionMapperTest {
  TileValidator tileValidator = new TileValidator();
  DetectionMapper subject = new DetectionMapper(tileValidator);

  DetectionResponse detectionResponse() {
    return DetectionResponse.builder().rstRaw(Map.of("filename", imageData())).build();
  }

  DetectionResponse.ImageData imageData() {
    return DetectionResponse.ImageData.builder()
        .base64ImgData("image_base_64")
        .regions(Map.of("regions", region()))
        .build();
  }

  DetectionResponse.ImageData.Region region() {
    return DetectionResponse.ImageData.Region.builder()
        .regionAttributes(Map.of("label", "POOL", "confidence", "0.95"))
        .shapeAttributes(shapes())
        .build();
  }

  DetectionResponse.ImageData.ShapeAttributes shapes() {
    return DetectionResponse.ImageData.ShapeAttributes.builder()
        .allPointsX(
            List.of(BigDecimal.valueOf(6.958009303660302), BigDecimal.valueOf(43.543013820437459)))
        .allPointsY(
            List.of(BigDecimal.valueOf(6.957965493371299), BigDecimal.valueOf(43.543002082885863)))
        .build();
  }

  @Test
  void map_to_detection_response_to_detected_tile() {
    var parcelId = randomUUID().toString();
    var zoneJobId = randomUUID().toString();
    var parcelJobId = randomUUID().toString();
    var tile =
        Tile.builder()
            .id("tile_id")
            .coordinates(new TileCoordinates().x(5000).y(2000).z(20))
            .build();
    var actual =
        subject.toDetectedTile(detectionResponse(), tile, parcelId, zoneJobId, parcelJobId);

    assertNotNull(actual);
    assertFalse(actual.getDetectedObjects().isEmpty());
    assertNotNull(actual.getFirstObject());
    assertEquals(tile, actual.getTile());
    assertEquals(parcelId, actual.getParcelId());
    assertEquals(zoneJobId, actual.getZdjJobId());
    assertEquals(parcelJobId, actual.getParcelJobId());
  }

  @Test
  void map_annotation_to_human_detected_object() {
    var zoom = 20;
    var tileId = randomUUID().toString();

    var actual = subject.toHumanDetectedObject(zoom, tileId, List.of(annotation()));
    var detectedObject = actual.getFirst();

    assertFalse(actual.isEmpty());
    assertEquals(PISCINE, detectedObject.getDetectableObjectType());
    assertEquals(HUMAN, detectedObject.getType());
    assertEquals(tileId, detectedObject.getDetectedTileId());
    assertEquals(0.9515481, detectedObject.getComputedConfidence());
    assertEquals(feature(), detectedObject.getFeature().id(null));
  }

  private Annotation annotation() {
    return new Annotation()
        .comment("confidence=95.15481")
        .label(new Label().id(randomUUID().toString()).name("POOL").color("#fffff"))
        .polygon(new Polygon().points(List.of(new Point().y(500.0).x(147.5))));
  }

  private Feature feature() {
    var coordinates =
        List.of(List.of(List.of(List.of(new BigDecimal("147.5"), new BigDecimal("500.0")))));
    return new Feature().id(null).zoom(20).geometry(new MultiPolygon().coordinates(coordinates));
  }
}
