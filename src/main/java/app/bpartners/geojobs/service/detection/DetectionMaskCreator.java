package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon.toPixel;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.FeatureGeometry;
import app.bpartners.geojobs.endpoint.rest.model.MultiPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.repository.model.Feature;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DetectionMaskCreator implements Function<List<Feature>, File> {
  private static final int DEFAULT_IMAGE_SIZE = 1024;

  private List<IntXY> mapToPixel(List<Feature> providedGeojson) {
    int zoom = providedGeojson.get(0).getZoom();

    var typedMercatorCoords =
        providedGeojson.stream()
            .map(FeatureMapper::toRestFeature)
            .map(app.bpartners.geojobs.endpoint.rest.model.Feature::getGeometry)
            .filter(Objects::nonNull)
            .map(FeatureGeometry::getMultiPolygon)
            .map(MultiPolygon::getCoordinates)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .map(list -> new LatLon(list.get(1).doubleValue(), list.get(0).doubleValue()))
            .toList();

    return typedMercatorCoords.stream()
        .map(latLon -> toPixel(latLon, new TilingConf(zoom, DEFAULT_IMAGE_SIZE)))
        .toList();
  }

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

    File output = File.createTempFile("mask" + randomUUID(), ".png", createTempDirectory());
    ImageIO.write(image, "png", output);
    log.info("Mask saved at {}", output.getAbsolutePath());
    return output;
  }

  @Override
  public File apply(List<Feature> features) {
    var pixels = mapToPixel(features);
    return drawImage(pixels);
  }
}
