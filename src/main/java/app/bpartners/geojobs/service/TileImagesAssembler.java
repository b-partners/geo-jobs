package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.io.File.createTempFile;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static java.util.UUID.randomUUID;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageIO.write;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TileImagesAssembler implements Function<List<Tile>, File> {
  @SneakyThrows
  @Override
  public File apply(List<Tile> tiles) {
    if (tiles == null || tiles.isEmpty()) {
      throw new IllegalArgumentException("Tiles must not be empty");
    }
    var sortedTiles =
        tiles.stream()
            .sorted(
                Comparator.comparing((Tile t) -> t.getCoordinates().getZ())
                    .thenComparing(t -> t.getCoordinates().getY())
                    .thenComparing(t -> t.getCoordinates().getX()))
            .toList();

    var grid = computeGridInfo(sortedTiles);
    var canvas = new BufferedImage(grid.totalWidth(), grid.totalHeight(), TYPE_INT_RGB);
    var graphics = canvas.createGraphics();

    sortedTiles.forEach(
        tile -> {
          BufferedImage tileImage;
          try {
            tileImage = read(tile.getImage());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          int posX = (tile.getCoordinates().getX() - grid.minX()) * grid.tileWidth();
          int posY = (tile.getCoordinates().getY() - grid.minY()) * grid.tileHeight();
          graphics.drawImage(tileImage, posX, posY, null);
        });

    graphics.dispose();

    return createJpegFile(canvas);
  }

  @SneakyThrows
  private File createJpegFile(BufferedImage bufferedAssembleImage) {
    var assembleImageFile =
        createTempFile("assemble_image_" + randomUUID(), ".jpg", createTempDirectory());
    write(bufferedAssembleImage, "jpg", assembleImageFile);
    log.info("Image assembled created at {}", assembleImageFile.getAbsolutePath());
    return assembleImageFile;
  }

  @SneakyThrows
  private GridInfo computeGridInfo(List<Tile> tiles) {
    int minX = MAX_VALUE, minY = MAX_VALUE;
    int maxX = MIN_VALUE, maxY = MIN_VALUE;
    int tileWidth = 0, tileHeight = 0;

    for (Tile tile : tiles) {
      TileCoordinates c = tile.getCoordinates();
      minX = Math.min(minX, c.getX());
      minY = Math.min(minY, c.getY());
      maxX = Math.max(maxX, c.getX());
      maxY = Math.max(maxY, c.getY());

      if (tileWidth == 0 || tileHeight == 0) {
        BufferedImage img = read(tile.getImage());
        tileWidth = img.getWidth();
        tileHeight = img.getHeight();
      }
    }

    int totalWidth = (maxX - minX + 1) * tileWidth;
    int totalHeight = (maxY - minY + 1) * tileHeight;

    return new GridInfo(minX, minY, tileWidth, tileHeight, totalWidth, totalHeight);
  }

  private record GridInfo(
      int minX, int minY, int tileWidth, int tileHeight, int totalWidth, int totalHeight) {}
}
