package app.bpartners.geojobs.service.lidar;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.lidar.planes.model.LasRoofDelimitationType.ENTIRE_ROOF_DELIMITATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.service.GeometrySquareMeterArea;
import app.bpartners.geojobs.service.lidar.api.LidarApiFacade;
import app.bpartners.geojobs.service.lidar.api.SwissBoundaryChecker;
import app.bpartners.geojobs.utils.lidar.LasRoofsPointsExtractorCreator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

@Slf4j
class LasRoofPointsExtractorTest {
  private static final String LARGE_LIDAR_FILE_PATH =
      "las/LHD_FXX_0644_6859_PTS_O_LAMB93_IGN69.copc.laz";

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
    var error =
        assertThrows(
            IllegalStateException.class,
            () -> processor.apply(ENTIRE_ROOF_DELIMITATION, roofGeometries));

    assertTrue(error.getMessage().contains("Roof found but no BATIMENT points"));
  }

  @Test
  void should_work_correctly_with_one_polygon_as_delimitation() {
    var roof1 = roofGeometry1();
    var pointsExtractor =
        LasRoofsPointsExtractorCreator.create(roof1, List.of(LARGE_LIDAR_FILE_PATH));

    var result = pointsExtractor.apply(ENTIRE_ROOF_DELIMITATION, Set.of(roof1));

    var roof1Points = result.extract(roof1);

    assertEquals(3487, roof1Points.getItems()[0].getPoints().size());
    assertEquals(101, roof1Points.getGroundPoints().size());
  }

  @Test
  void should_work_correctly_with_one_multipolygon_as_delimitation() {
    var roof1 = (Polygon) roofGeometry1();
    var multipolygon = geometryFactory.createMultiPolygon(new Polygon[] {roof1});
    var pointsExtractor =
        LasRoofsPointsExtractorCreator.create(multipolygon, List.of(LARGE_LIDAR_FILE_PATH));

    var result = pointsExtractor.apply(ENTIRE_ROOF_DELIMITATION, Set.of(multipolygon));

    var roof1Points = result.extract(multipolygon);

    assertEquals(3487, roof1Points.getItems()[0].getPoints().size());
    assertEquals(101, roof1Points.getGroundPoints().size());
  }

  private static SwissBoundaryChecker swissBoundaryCheckerMock() {
    var swissBoundaryChecker = mock(SwissBoundaryChecker.class);
    when(swissBoundaryChecker.isGeometryInSwiss(any())).thenReturn(false);
    return swissBoundaryChecker;
  }

  private static Geometry roofOutsideLidar() {
    var coordinates =
        new Coordinate[] {
          new Coordinate(144.97294507706766, -37.81153946626492),
          new Coordinate(144.9725373216853, -37.811633860605596),
          new Coordinate(144.97267007925188, -37.811914046267916),
          new Coordinate(144.97306835195047, -37.81181515733155),
          new Coordinate(144.97294507706766, -37.81153946626492)
        };
    return geometryFactory.createPolygon(coordinates);
  }

  private static Geometry roofGeometry1() {
    var roof1Coordinates =
        new Coordinate[] {
          new Coordinate(2.243891733457616, 48.82448842864014),
          new Coordinate(2.243947393505863, 48.82437718542337),
          new Coordinate(2.244038835011281, 48.82440597780899),
          new Coordinate(2.2440209442821413, 48.82445309258651),
          new Coordinate(2.244197863717403, 48.8244975898354),
          new Coordinate(2.24422768160008, 48.82447010624497),
          new Coordinate(2.24432906240051, 48.824487119898066),
          new Coordinate(2.244263463059525, 48.82456695311532),
          new Coordinate(2.243891733457616, 48.82448842864014)
        };
    return geometryFactory.createPolygon(roof1Coordinates);
  }
}
