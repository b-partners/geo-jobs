package app.bpartners.geojobs.service.mapper;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.repository.model.DetectableObjectType;
import app.bpartners.geojobs.repository.model.DetectedObject;
import app.bpartners.geojobs.repository.model.DetectedTile;
import app.bpartners.geojobs.repository.model.Tile;
import app.bpartners.geojobs.service.geo.response.DetectionResponse;
import app.bpartners.geojobs.service.validator.TileValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class DetectionMapper {
  private static final TileValidator tileValidator = new TileValidator();

  public static DetectedTile toDetectedTile(DetectionResponse detectionResponse, Tile tile) {
    String detectedTileId = randomUUID().toString();
    var tileCoordinates = tile.getCoordinates();
    tileValidator.accept(tile);

    var fileData = detectionResponse.getRstRaw().values().stream().toList().get(0); //

    List<DetectionResponse.ImageData.Region> regions =
        fileData.getRegions().values().stream().toList();
    List<DetectedObject> detectedObjects =
        regions.stream()
            .map(region -> toDetectedObject(region, detectedTileId, tileCoordinates.getZ()))
            .toList();

    return DetectedTile.builder()
        .id(detectedTileId)
        .tile(tile)
        .detectedObjects(detectedObjects)
        .creationDatetime(Instant.now())
        .build();
  }

  public static DetectedObject toDetectedObject(
      DetectionResponse.ImageData.Region region, String detectedTileId, Integer zoom) {
    var label = region.getRegionAttributes().values().stream().toList().get(0);
    var polygon = region.getShapeAttributes();
    var objectId = randomUUID().toString();
    return DetectedObject.builder()
        .id(objectId)
        .detectedTileId(detectedTileId)
        .detectedObjectTypes(
            List.of(
                DetectableObjectType.builder()
                    .id(randomUUID().toString())
                    .objectId(objectId)
                    .detectableType(toDetectableType(label))
                    .build()))
        .feature(toFeature(polygon, zoom))
        .build();
  }

  private static DetectableObjectType.DetectableType toDetectableType(String label) {
    return switch (label.toUpperCase()) {
      case "ROOF" -> DetectableObjectType.DetectableType.ROOF;
      case "SOLAR_PANEL" -> DetectableObjectType.DetectableType.SOLAR_PANEL;
      case "TREE" -> DetectableObjectType.DetectableType.TREE;
      case "PATHWAY" -> DetectableObjectType.DetectableType.PATHWAY;
      case "POOL" -> DetectableObjectType.DetectableType.POOL;
      default -> throw new IllegalStateException("Unexpected value: " + label.toLowerCase());
    };
  }

  private static Feature toFeature(
      DetectionResponse.ImageData.ShapeAttributes shapeAttributes, int zoom) {
    List<List<BigDecimal>> coordinates = new ArrayList<>();
    var allX = shapeAttributes.getAllPointsX();
    var allY = shapeAttributes.getAllPointsY();
    IntStream.range(0, allX.size())
        .forEach(i -> coordinates.add(List.of(allX.get(i), allY.get(i))));
    return new Feature()
        .id(randomUUID().toString())
        .zoom(zoom)
        .geometry(
            new MultiPolygon()
                .type(MultiPolygon.TypeEnum.POLYGON)
                .coordinates(List.of(List.of(coordinates))));
  }
}
