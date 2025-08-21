package app.bpartners.geojobs.service;

import static javax.imageio.ImageIO.read;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TileImagesAssemblerTest {
  TileImagesAssembler subject = new TileImagesAssembler();

  @SneakyThrows
  @Test
  void tiles_assemble_to_one_image() {
    var zoom = 20;

    var actual =
        subject.apply(
            List.of(
                Tile.builder()
                    .image(new ClassPathResource("/images/extender/61-92.jpg").getFile())
                    .coordinates(new TileCoordinates().x(523561).y(370292).z(zoom))
                    .build(),
                Tile.builder()
                    .image(new ClassPathResource("/images/extender/62-92.jpg").getFile())
                    .coordinates(new TileCoordinates().x(523562).y(370292).z(zoom))
                    .build(),
                Tile.builder()
                    .image(new ClassPathResource("/images/extender/63-92.jpg").getFile())
                    .coordinates(new TileCoordinates().x(523563).y(370292).z(zoom))
                    .build(),
                Tile.builder()
                    .image(new ClassPathResource("/images/extender/61-93.jpg").getFile())
                    .coordinates(new TileCoordinates().x(523561).y(370293).z(zoom))
                    .build()));

    assertNotNull(actual);
    assertTrue(
        compareImages(actual, new ClassPathResource("/images/assemble_image.jpg").getFile()));
  }

  @SneakyThrows
  public static boolean compareImages(File actual, File expected) {
    BufferedImage img1 = read(actual);
    BufferedImage img2 = read(expected);

    if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
      return false;
    }

    for (int y = 0; y < img1.getHeight(); y++) {
      for (int x = 0; x < img1.getWidth(); x++) {
        if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
          return false;
        }
      }
    }

    return true;
  }
}
