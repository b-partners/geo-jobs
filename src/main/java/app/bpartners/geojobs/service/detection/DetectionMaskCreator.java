package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.originTile;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.toPixel;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionMaskCreator implements Function<List<Feature>, Map<IntXY, File>> {
  private static final int DEFAULT_IMAGE_SIZE = 1024 * 3;

  private List<IntXY> mapToPixel(
      List<List<BigDecimal>> providedGeojson, IntXY originTile, TilingConf tilingConf) {
    var typedMercatorCoords =
        providedGeojson.stream()
            .map(list -> new LatLon(list.get(1).doubleValue(), list.getFirst().doubleValue()))
            .toList();

    return typedMercatorCoords.stream()
        .map(latLon -> toPixel(latLon, tilingConf, originTile))
        .toList();
  }

  @SneakyThrows
  private BufferedImage drawImage(List<IntXY> pixels) {
    BufferedImage image =
        new BufferedImage(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = image.createGraphics();

    g2d.setColor(BLACK);
    g2d.fillRect(0, 0, DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE);

    if (!pixels.isEmpty()) {
      int[] xPoints = pixels.stream().mapToInt(IntXY::x).toArray();
      int[] yPoints = pixels.stream().mapToInt(IntXY::y).toArray();

      if (xPoints.length > 2) {
        g2d.setColor(WHITE);
        g2d.fillPolygon(xPoints, yPoints, xPoints.length);

        g2d.setColor(WHITE);
        g2d.drawPolygon(xPoints, yPoints, xPoints.length);
      }
    }
    g2d.dispose();
    return image;
  }

  @SneakyThrows
  private Map<IntXY, File> split_image(IntXY originTile, BufferedImage fullImage) {
    var divider = fullImage.getWidth() / 1024;
    var outputFiles = new HashMap<IntXY, File>();

    for (int row = 0; row < divider; row++) {
      for (int col = 0; col < divider; col++) {
        int x = col * 1024;
        int y = row * 1024;

        BufferedImage subImage = fullImage.getSubimage(x, y, 1024, 1024);

        File output =
            File.createTempFile(
                "mask_" + row + "_" + col + "_" + randomUUID(), ".png", createTempDirectory());
        ImageIO.write(subImage, "png", output);
        var tile = new IntXY(originTile.x() + col, originTile.y() + row);
        log.info("Sub-image {} saved at {}", tile, output.getAbsolutePath());
        outputFiles.put(tile, output);
      }
    }
    return outputFiles;
  }

  @Override
  public Map<IntXY, File> apply(List<Feature> providedGeoJson) {
    int zoom = providedGeoJson.getFirst().getZoom();
    var flattedFeatures =
        providedGeoJson.stream()
            .map(app.bpartners.geojobs.endpoint.rest.model.Feature::getGeometry)
            .filter(Objects::nonNull)
            .map(FeatureGeometry::getMultiPolygon)
            .map(MultiPolygon::getCoordinates)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .toList();
    var refLat =
        Collections.max(flattedFeatures.stream().map(list -> list.get(1)).toList()).doubleValue();
    var refLon =
        Collections.min(flattedFeatures.stream().map(List::getFirst).toList()).doubleValue();
    var originTile = originTile(new Coordinate(refLat, refLon), zoom);
    var tilingConf = new TilingConf(zoom, DEFAULT_IMAGE_SIZE);
    var pixels = mapToPixel(flattedFeatures, originTile, tilingConf);
    var fullImage = drawImage(pixels);
    return split_image(originTile, fullImage);
  }
}
