package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Slf4j
class LasRoofPointsExtractorTest {
  @Test
  void should_failed_if_batiment_points_count_is_less_than_twenty() {
    var apiMock = mock(LidarApiFacade.class);
    var processor =
        spy(
            new LasRoofsPointsExtractor(
                apiMock, new GeometrySquareMeterArea(), swissBoundaryCheckerMock()));

    var roofGeometry1 = roofOutsideLidar();
    when(apiMock.getUniqueLidarFilesUrls(any()))
        .thenReturn(Map.of("file.laz", Set.of(roofOutsideLidar())));

    var roofGeometries = Set.of(roofGeometry1);
    var error = assertThrows(IllegalStateException.class, () -> processor.apply(ENTIRE_ROOF_DELIMITATION, roofGeometries));

    assertTrue(error.getMessage().contains("Roof found but no BATIMENT points"));
  }

  private static SwissBoundaryChecker swissBoundaryCheckerMock() {
    var swissBoundaryChecker = mock(SwissBoundaryChecker.class);
    when(swissBoundaryChecker.isGeometryInSwiss(any())).thenReturn(false);
    return swissBoundaryChecker;
  }

  private static Geometry roofOutsideLidar(){
    var coordinates = new Coordinate[] {
      new Coordinate(144.97294507706766, -37.81153946626492),
      new Coordinate(144.9725373216853, -37.811633860605596),
      new Coordinate(144.97267007925188, -37.811914046267916),
      new Coordinate(144.97306835195047, -37.81181515733155),
      new Coordinate(144.97294507706766, -37.81153946626492)
    };
    return geometryFactory.createPolygon(coordinates);
  }
}
