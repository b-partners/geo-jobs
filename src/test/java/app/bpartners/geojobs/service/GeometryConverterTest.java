package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.service.geojson.GeometryConverter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class GeometryConverterTest {
  private static final double AREA_TOLERANCE = 0.001;

  GeometryConverter subject = new GeometryConverter();

  @Test
  void retrieve_geometry_from_tile_coordinates() {
    var actual = subject.getMultiPolygonFromTile(544680, 383095, 20);

    var actualGeometryAsString = subject.writeGeometryAsString(actual);
    assertEquals(expectedGeometryFromTileCoordinates(), actualGeometryAsString);
  }

  private String expectedGeometryFromTileCoordinates() {
    return "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[7.00103759765625,43.55053877556738],[7.00103759765625,43.55078760402636],[7.001380920410156,43.55078760402636],[7.001380920410156,43.55053877556738],[7.00103759765625,43.55053877556738]]]]}";
  }

  @Test
  void convert_valid_ring_to_polygon() {
    var actual = subject.convertToPolygon(ring(0, 0, 10, 0, 10, 10, 0, 10, 0, 0));

    assertTrue(actual.isValid());
    assertEquals(100.0, actual.getArea(), AREA_TOLERANCE);
  }

  @Test
  void keep_the_widest_part_of_a_self_intersecting_ring() {
    var selfIntersectingSeveralTimes = ring(0, 0, 6, 6, 0, 6, 6, 0, 0, 0, 30, 3, 0, 3, 0, 0);

    var actual = subject.convertToPolygon(selfIntersectingSeveralTimes);

    assertTrue(actual.isValid(), "the repaired polygon is expected to be valid");
    assertEquals(
        33.136, actual.getArea(), AREA_TOLERANCE, "the widest part is expected to be kept");
  }

  @Test
  void keep_the_hole_of_a_ring_looping_inside_itself() {
    var ringLoopingInsideItself =
        ring(0, 0, 10, 0, 10, 10, 0, 10, 0, 0, 2, 2, 2, 8, 8, 8, 8, 2, 2, 2, 0, 0);

    var actual = subject.convertToPolygon(ringLoopingInsideItself);

    assertTrue(actual.isValid(), "the repaired polygon is expected to be valid");
    assertEquals(1, actual.getNumInteriorRing(), "the loop is expected to be kept as a hole");
    assertEquals(64.0, actual.getArea(), AREA_TOLERANCE, "the hole is expected to be subtracted");
  }

  @Test
  void drop_the_zero_width_spike_of_a_ring() {
    var ringWithZeroWidthSpike = ring(0, 0, 10, 0, 10, 10, 0, 10, 0, 0, 15, 0, 0, 0);

    var actual = subject.convertToPolygon(ringWithZeroWidthSpike);

    assertTrue(actual.isValid(), "the repaired polygon is expected to be valid");
    assertEquals(100.0, actual.getArea(), AREA_TOLERANCE, "the spike holds no surface");
  }

  @Test
  void reject_a_ring_of_less_than_four_points() {
    assertThrows(
        IllegalArgumentException.class, () -> subject.convertToPolygon(ring(0, 0, 10, 0, 0, 0)));
  }

  private List<List<BigDecimal>> ring(double... xy) {
    List<List<BigDecimal>> points = new ArrayList<>();
    for (int i = 0; i < xy.length / 2; i++) {
      points.add(List.of(BigDecimal.valueOf(xy[2 * i]), BigDecimal.valueOf(xy[2 * i + 1])));
    }
    return points;
  }
}
