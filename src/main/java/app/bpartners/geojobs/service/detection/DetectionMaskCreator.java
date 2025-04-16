package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.originTile;
import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.toPixel;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toSet;

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
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriFunction;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionMaskCreator implements TriFunction<List<List<BigDecimal>>, IntXY, Integer, File> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;

  @SneakyThrows
  private File drawImage(List<IntXY> pixels) {
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
    File output =
            File.createTempFile(
                    "mask_" + randomUUID(), ".png", createTempDirectory());
    ImageIO.write(image, "png", output);
    return output;
  }

  @Override
  public File apply(List<List<BigDecimal>> providedGeoJson, IntXY originTile, Integer zoom) {
    var tilingConf = new TilingConf(zoom, DEFAULT_IMAGE_SIZE);

    var typedMercatorCoords = providedGeoJson.stream()
                    .map(list -> new LatLon(list.get(1).doubleValue(), list.getFirst().doubleValue()))
                    .toList();

    var pixels =  typedMercatorCoords.stream()
            .map(latLon -> toPixel(latLon, tilingConf, originTile))
            .toList();
    return drawImage(pixels);
  }
}
