package app.bpartners.geojobs.model.lidar.api;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.lidar.CrsProjector;
import app.bpartners.geojobs.model.lidar.zone.LidarZone;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

class LidarApiUrlResolverTest {
  CrsProjector crsProjectorMock = mock(CrsProjector.class);
  LidarApiUrlResolver subject = new LidarApiUrlResolver(crsProjectorMock);

  @Test
  void resolves_urls_through_the_geometrys_zone() {
    var geometry = someGeometry();
    var zoneMock = mock(LidarZone.class);
    var expectedUrls = Set.of("https://data.geopf.fr/dummy.laz");
    when(crsProjectorMock.resolveZone(geometry)).thenReturn(zoneMock);
    when(zoneMock.resolveUrls(geometry.getEnvelopeInternal())).thenReturn(expectedUrls);

    var actual = subject.resolveUrls(geometry);

    assertEquals(expectedUrls, actual);
  }

  private static Geometry someGeometry() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(coordinates);
  }
}
