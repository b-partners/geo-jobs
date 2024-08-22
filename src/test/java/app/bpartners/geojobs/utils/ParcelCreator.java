package app.bpartners.geojobs.utils;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.ArrayList;
import java.util.List;

public class ParcelCreator {
  TileCreator tileCreator = new TileCreator();

  public Parcel create(int nbTilePerParcel) {
    if (nbTilePerParcel < 0)
      throw new RuntimeException("nbTilePerParcel must be > 0 but was " + nbTilePerParcel);
    var tiles = new ArrayList<Tile>();
    for (int j = 0; j < nbTilePerParcel; j++) {
      tiles.add(tileCreator.create(randomUUID().toString(), "bucketPath-" + randomUUID()));
    }
    return create(randomUUID().toString(), tiles);
  }

  private Parcel create(String id, Tile tile) {
    return create(id, List.of(tile));
  }

  public Parcel create(String id, List<Tile> tiles) {
    return Parcel.builder()
        .id(id)
        .parcelContent(ParcelContent.builder().tiles(tiles).build())
        .build();
  }
}
