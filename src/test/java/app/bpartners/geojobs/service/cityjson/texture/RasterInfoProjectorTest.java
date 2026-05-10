package app.bpartners.geojobs.service.cityjson.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.math.Vector3D;

class RasterInfoProjectorTest {

  RasterInfoProjector subject = new RasterInfoProjector();

  @Test
  void project_wgs84_to_lambert93() {
    // A point in France: 2.3522, 48.8566 (Paris) in WGS84 (lon, lat)
    List<Vector3D> source = List.of(new Vector3D(2.3522, 48.8566, 10.0));

    List<Vector3D> projected = subject.project(source, "EPSG:4326", "EPSG:2154");

    // Expected Lambert-93 (EPSG:2154) coordinates for Paris (2.3522, 48.8566)
    // X ≈ 652469.0
    // Y ≈ 6862035.2
    assertEquals(652469.0, projected.get(0).getX(), 1.0);
    assertEquals(6862035.2, projected.get(0).getY(), 1.0);
    assertEquals(10.0, projected.get(0).getZ());
  }

  @Test
  void project_same_crs() {
    List<Vector3D> source = List.of(new Vector3D(1.0, 2.0, 3.0));
    List<Vector3D> projected = subject.project(source, "EPSG:4326", "EPSG:4326");

    assertEquals(source, projected);
  }
}
