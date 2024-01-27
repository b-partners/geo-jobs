package app.bpartners.geojobs.service.validator;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.geo.tiling.Tile;
import java.util.function.Consumer;

public class TileValidator implements Consumer<Tile> {

  @Override
  public void accept(Tile tile) {
    var builder = new StringBuilder();
    var tileCoordinates = tile.getCoordinates();
    if (tileCoordinates.getX() == null) {
      builder.append("Xtile is mandatory. ");
    }
    if (tileCoordinates.getY() == null) {
      builder.append("Ytile is mandatory. ");
    }
    if (tileCoordinates.getZ() == null) {
      builder.append("Zoom is mandatory.");
    }
    if (!builder.isEmpty()) {
      throw new BadRequestException(builder.toString());
    }
  }
}
