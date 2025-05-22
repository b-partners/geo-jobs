package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Disabled("TODO: local use only")
@Slf4j
class VggPolygonDrawer {

  @SneakyThrows
  @Test
  void draw_polygon_on_file() {
    File vggFile = new ClassPathResource("vgg/annotations-vgg.json").getFile();
    File imageFile = new ClassPathResource("images/extender/extended_image.jpg").getFile();

    var actual = drawPolygonsOnImage(vggFile, imageFile);

    assertNotNull(actual);
    actual.delete();
  }

  private File drawPolygonsOnImage(File vggFile, File imageFile) throws Exception {
    // Lire l'image
    BufferedImage image = ImageIO.read(imageFile);
    Graphics2D g2d = image.createGraphics();
    g2d.setColor(Color.RED);
    g2d.setStroke(new BasicStroke(2));

    // Lire le fichier JSON
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(vggFile);

    Iterator<Map.Entry<String, JsonNode>> fileEntries = root.fields();
    while (fileEntries.hasNext()) {
      Map.Entry<String, JsonNode> fileEntry = fileEntries.next();
      JsonNode regions = fileEntry.getValue().get("regions");

      if (regions != null && regions.isObject()) {
        Iterator<Map.Entry<String, JsonNode>> regionEntries = regions.fields();
        while (regionEntries.hasNext()) {
          Map.Entry<String, JsonNode> regionEntry = regionEntries.next();
          JsonNode shape = regionEntry.getValue().get("shape_attributes");

          if (shape != null && "polygon".equalsIgnoreCase(shape.get("name").asText())) {
            JsonNode xPointsNode = shape.get("all_points_x");
            JsonNode yPointsNode = shape.get("all_points_y");

            int numPoints = xPointsNode.size();
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];

            for (int i = 0; i < numPoints; i++) {
              xPoints[i] = xPointsNode.get(i).asInt();
              yPoints[i] = yPointsNode.get(i).asInt();
            }

            Polygon polygon = new Polygon(xPoints, yPoints, numPoints);
            g2d.drawPolygon(polygon);
          }
        }
      }
    }

    g2d.dispose();

    File outputFile = new File("image_annotated.jpg");
    ImageIO.write(image, "jpg", outputFile);
    return outputFile;
  }
}
