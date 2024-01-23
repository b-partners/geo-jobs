package school.hei.geojobs.service.validator;

import java.util.function.Consumer;
import school.hei.geojobs.model.exception.BadRequestException;
import school.hei.geojobs.repository.model.Tile;

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
