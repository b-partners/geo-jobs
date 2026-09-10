package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.auvergneRhoneAlpes;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.switzerland_coords;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.geojobs.model.lidar.zone.DefaultZone;
import app.bpartners.geojobs.model.lidar.zone.SwissZone;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import org.junit.jupiter.api.Test;

class CrsProjectorTest {
  GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  CrsProjector subject = CrsProjector.INSTANCE;

  @Test
  void instance_is_a_singleton() {
    assertSame(CrsProjector.INSTANCE, CrsProjector.INSTANCE);
  }

  @Test
  void swiss_geometry_resolves_to_swiss_zone_and_epsg2056() {
    var wgs84Geometry = switzerland_coords();

    assertSame(SwissZone.INSTANCE, subject.resolveZone(wgs84Geometry));
    assertTrue(subject.isInSwiss(wgs84Geometry));
    assertEquals(EPSG_2056, subject.getLocalCrs(wgs84Geometry));
    assertEquals(
        projector.project(wgs84Geometry, WGS84, EPSG_2056), subject.projectToLocalCrs(wgs84Geometry));
  }

  @Test
  void french_geometry_resolves_to_default_zone_and_lambert93() {
    var wgs84Geometry = auvergneRhoneAlpes();

    assertSame(DefaultZone.INSTANCE, subject.resolveZone(wgs84Geometry));
    assertFalse(subject.isInSwiss(wgs84Geometry));
    assertEquals(LAMBERT_93, subject.getLocalCrs(wgs84Geometry));
    assertEquals(
        projector.project(wgs84Geometry, WGS84, LAMBERT_93), subject.projectToLocalCrs(wgs84Geometry));
  }
}
