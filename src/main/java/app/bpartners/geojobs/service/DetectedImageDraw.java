package app.bpartners.geojobs.service;

import static java.awt.Color.*;
import static java.awt.Font.BOLD;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

import app.bpartners.geojobs.model.DetectedObjectTypeWithPolygon;
import app.bpartners.geojobs.repository.model.detection.DetectableType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class DetectedImageDraw
    implements BiFunction<File, List<DetectedObjectTypeWithPolygon>, File> {

  @SneakyThrows
  @Override
  public File apply(
      File originalImage, List<DetectedObjectTypeWithPolygon> detectedObjectTypeWithPolygon) {
    BufferedImage image = ImageIO.read(originalImage);
    if (image == null)
      throw new IOException("Not valid image file found for " + originalImage.getName());
    Graphics2D g2d = image.createGraphics();
    g2d.setStroke(new BasicStroke(4));
    g2d.setFont(new Font("Arial", BOLD, 14));
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON); // For best rendering

    for (DetectedObjectTypeWithPolygon polygon : detectedObjectTypeWithPolygon) {
      g2d.setColor(getColor(polygon.objectType()));
      List<app.bpartners.geojobs.endpoint.rest.model.Point> points = polygon.pointList();
      int[] xPoints =
          points.stream().mapToInt(point -> point.getCoordinates().getFirst().intValue()).toArray();
      int[] yPoints =
          points.stream().mapToInt(point -> point.getCoordinates().getLast().intValue()).toArray();
      if (points.size() >= 3) {
        g2d.drawPolygon(xPoints, yPoints, points.size());
      } else if (points.size() == 2) {
        g2d.drawLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
      } else if (points.size() == 1) {
        g2d.fillOval(xPoints[0] - 2, yPoints[0] - 2, 4, 4);
      }

      // Write object type label
      g2d.drawString(polygon.objectType().toString(), xPoints[0], yPoints[0] - 5);
    }

    g2d.dispose();

    var outputFile = Files.createTempFile(originalImage.getName(), ".jpg").toFile();
    ImageIO.write(image, "jpg", outputFile);
    return outputFile;
  }

  // TODO : set specific color for each type
  private Color getColor(DetectableType detectableType) {
    return switch (detectableType) {
      case HUMIDITE, HUMIDITE_CLAIR, HUMIDITE_INTENSE -> BLUE;
      case MOISISSURE, MOISISSURE_CLAIR, MOISISSURE_COULEUR, MOISISSURE_NOIRCIE -> YELLOW;
      case OBSTACLE, CHEMINEE, PANNEAU_PHOTOVOLTAIQUE, VELUX -> ORANGE;
      case USURE, USURE_IMPORTANTE, USURE_LEGER -> RED;
      default -> GRAY;
    };
  }
}
