package app.bpartners.geojobs.service.gouv.fr.rnb;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.service.GeometryTools;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.Building;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.BuildingClosest;
import app.bpartners.geojobs.service.gouv.fr.rnb.component.geometry.Geometry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeometryConverterITTest {
  BuildingApi buildingApi = mock(BuildingApi.class);
  GeometryTools geometryTools = mock(GeometryTools.class);
  GeometryConverter subject = new GeometryConverter(buildingApi);

  @Test
  void retrieveRoofPolygonsFrom_returnsPolygons() {
    var radius = 80;
    var mockGeometry = mock(Geometry.class);
    var mockBuilding = new Building("rnb_id", null, null, null, null, 0.0);
    var buildingClosest = new BuildingClosest(null, null, List.of(mockBuilding), 10.0);
    var fullBuilding = new Building("rnb_id", null, null, mockGeometry, null, 10.0);

    List<List<BigDecimal>> polygonCoordinates =
        List.of(
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)));
    when(geometryTools.getMinimumEnclosingRadius(polygonCoordinates)).thenReturn(radius);
    when(buildingApi.getBuildingClosest(anyDouble(), anyDouble(), anyInt()))
        .thenReturn(buildingClosest);
    when(mockGeometry.getMultiPolygonCoordinates())
        .thenReturn(List.of(List.of(polygonCoordinates)));
    when(buildingApi.getBuildingByRnbId("rnb_id")).thenReturn(fullBuilding);

    var actual = subject.retrieveRoofPolygonsFrom(polygonCoordinates);

    assertAll(
        "roof polygons",
        () -> assertFalse(actual.isEmpty()),
        () -> assertEquals(1, actual.size()),
        () -> assertEquals("MultiPolygon", actual.getFirst().getGeometryType()));

    verify(buildingApi).getBuildingClosest(anyDouble(), anyDouble(), anyInt());
    verify(buildingApi).getBuildingByRnbId("rnb_id");
    verify(mockGeometry).getMultiPolygonCoordinates();
  }

  @Test
  void retrieveRoofPolygonsFrom_throwsException()
      throws NoSuchFieldException, IllegalAccessException {
    var field = GeometryConverter.class.getDeclaredField("geometryTools");
    List<List<BigDecimal>> polygonCoordinates =
        List.of(
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.651375009133034)),
            List.of(
                BigDecimal.valueOf(-0.24774244579347737), BigDecimal.valueOf(46.652159755838795)),
            List.of(
                BigDecimal.valueOf(-0.24945104029509935), BigDecimal.valueOf(46.652159755838795)));
    field.setAccessible(true);
    field.set(subject, geometryTools);

    when(geometryTools.getMinimumEnclosingRadius(polygonCoordinates)).thenReturn(1500);

    UnsupportedOperationException exception =
        assertThrows(
            UnsupportedOperationException.class,
            () -> subject.retrieveRoofPolygonsFrom(polygonCoordinates));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "Provided multiPolygon zone is larger than supported retrieving roof polygons"
                    + " radius 1000, actual is "
                    + 1500));
    assertEquals(
        "Provided multiPolygon zone is larger than supported retrieving roof polygons radius 1000,"
            + " actual is 1500",
        exception.getMessage());
  }
}
