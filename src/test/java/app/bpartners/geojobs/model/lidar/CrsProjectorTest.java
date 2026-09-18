package app.bpartners.geojobs.model.lidar;

import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_2056;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.EPSG_3812;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.LAMBERT_93;
import static app.bpartners.geojobs.service.GeometrySquareMeterArea.WGS84;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.auvergneRhoneAlpes;
import static app.bpartners.geojobs.service.model.SwissBoundaryCheckerTest.switzerland_coords;
import static app.bpartners.geojobs.service.model.WalloniaBoundaryCheckerTest.liege_coords;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.model.lidar.api.IgnLidarApi;
import app.bpartners.geojobs.model.lidar.api.LidarUrlSafety;
import app.bpartners.geojobs.model.lidar.api.OpenSourceLidarApi;
import app.bpartners.geojobs.model.lidar.api.SwissLidarApi;
import app.bpartners.geojobs.model.lidar.zone.DefaultZone;
import app.bpartners.geojobs.model.lidar.zone.SwissZone;
import app.bpartners.geojobs.model.lidar.zone.WalloniaZone;
import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.api.WalloniaLidarApi;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrsProjectorTest {
  GeometrySquareMeterArea projector = new GeometrySquareMeterArea();
  LidarUrlSafety urlSafetyMock = mock(LidarUrlSafety.class);
  SwissZone swissZone = new SwissZone(mock(SwissLidarApi.class), urlSafetyMock);
  WalloniaZone walloniaZone = new WalloniaZone(mock(WalloniaLidarApi.class), urlSafetyMock);
  DefaultZone defaultZone =
      new DefaultZone(mock(OpenSourceLidarApi.class), mock(IgnLidarApi.class), urlSafetyMock);
  CrsProjector subject =
      new CrsProjector(List.of(swissZone, walloniaZone, defaultZone), projector);

  @Test
  void swiss_geometry_resolves_to_swiss_zone_and_epsg2056() {
    var wgs84Geometry = switzerland_coords();

    assertSame(swissZone, subject.resolveZone(wgs84Geometry));
    assertTrue(subject.isInSwiss(wgs84Geometry));
    assertEquals(EPSG_2056, subject.getLocalCrs(wgs84Geometry));
    assertEquals(
        projector.project(wgs84Geometry, WGS84, EPSG_2056),
        subject.projectToLocalCrs(wgs84Geometry));
  }

  @Test
  void wallonia_geometry_resolves_to_wallonia_zone_and_epsg3812() {
    var wgs84Geometry = liege_coords();

    assertSame(walloniaZone, subject.resolveZone(wgs84Geometry));
    assertTrue(subject.isInWallonia(wgs84Geometry));
    assertEquals(EPSG_3812, subject.getLocalCrs(wgs84Geometry));
    assertEquals(
        projector.project(wgs84Geometry, WGS84, EPSG_3812),
        subject.projectToLocalCrs(wgs84Geometry));
  }

  @Test
  void french_geometry_resolves_to_default_zone_and_lambert93() {
    var wgs84Geometry = auvergneRhoneAlpes();

    assertSame(defaultZone, subject.resolveZone(wgs84Geometry));
    assertFalse(subject.isInSwiss(wgs84Geometry));
    assertEquals(LAMBERT_93, subject.getLocalCrs(wgs84Geometry));
    assertEquals(
        projector.project(wgs84Geometry, WGS84, LAMBERT_93),
        subject.projectToLocalCrs(wgs84Geometry));
  }
}
