package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.postprocessing.Geojson;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.mockito.MockedStatic;

public class RoadContinuerServiceTest {

  private RoadContinuerService subject;

  @BeforeEach
  void setUp() {
    subject = new RoadContinuerService();
  }

  @Test
  void testGetGeoJsonFromString_createsTempFile() throws IOException {
    String input = "{\"type\": \"FeatureCollection\", \"features\": []}";
    File result = RoadContinuerService.getGeoJsonFromString(input);

    assertTrue(result.exists());
    assertTrue(result.getName().startsWith("continued-geojson-"));
  }

  @Test
  void continueRoute_ok() throws IOException {
    String input =
        """
        {
          "features" : [ ],
          "type" : "FeatureCollection"
        }""";
    TilingConf tilingConf = TilingConf.getDefaultInstance();
    File fileMock = File.createTempFile("geojson-test-", ".geojson");
    Files.writeString(fileMock.toPath(), input, StandardCharsets.UTF_8);

    GeometryFactory geometryFactory = new GeometryFactory();

    Coordinate[] coordinates =
        new Coordinate[] {
          new Coordinate(0, 0),
          new Coordinate(1, 0),
          new Coordinate(1, 1),
          new Coordinate(0, 1),
          new Coordinate(0, 0)
        };

    LinearRing shell = geometryFactory.createLinearRing(coordinates);
    Polygon polygon = geometryFactory.createPolygon(shell, null);

    LatLonPolygon latLonPolygon = new LatLonPolygon(polygon);

    try (MockedStatic<RoadContinuerService> mockedStatic =
        mockStatic(RoadContinuerService.class, CALLS_REAL_METHODS)) {
      mockedStatic
          .when(() -> RoadContinuerService.getGeoJsonFromString(input))
          .thenReturn(fileMock);

      LatLonLinesContinuer latLonLinesContinuerMock = mock(LatLonLinesContinuer.class);
      when(latLonLinesContinuerMock.apply(any(File.class))).thenReturn(Set.of(latLonPolygon));

      File expected = subject.continueRoute(input, tilingConf);
      assertNotNull(expected);
    }
  }

  @SneakyThrows
  @Test
  void test_ambohijatovo_crossed() {
    var resource = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");
    assertNotNull(resource);
    var file = new File(resource.toURI());
    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    var tilingConf = new TilingConf(17, 1_024);
    var continued = subject.continueRoute(content, tilingConf);

    var polygonSize = new Geojson(continued).polygons().size();

    assertTrue(continued.exists());
    assertEquals(1, polygonSize);
  }
}
