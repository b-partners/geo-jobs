package app.bpartners.geojobs.utils;

import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class ParcelCreator {
  ObjectMapper om = new ObjectMapper();
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

  @SneakyThrows
  public Parcel create(String id, List<Tile> tiles) {
    return Parcel.builder()
        .id(id)
        .parcelContent(
            ParcelContent.builder()
                .geoServerParameter(
                    om.readValue(
                        "{\n"
                            + "    \"service\": \"WMS\",\n"
                            + "    \"request\": \"GetMap\",\n"
                            + "    \"layers\": \"grandlyon:ortho_2018\",\n"
                            + "    \"styles\": \"\",\n"
                            + "    \"format\": \"image/png\",\n"
                            + "    \"transparent\": true,\n"
                            + "    \"version\": \"1.3.0\",\n"
                            + "    \"width\": 256,\n"
                            + "    \"height\": 256,\n"
                            + "    \"srs\": \"EPSG:3857\"\n"
                            + "  }",
                        GeoServerParameter.class))
                .tiles(tiles)
                .build())
        .build();
  }
}
