package app.bpartners.geojobs.repository.model.geo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ParcelTest {

  @Test
  void tiles_are_initialized_on_construction() {
    assertEquals(List.of(), new Parcel().getTiles());
  }
}
