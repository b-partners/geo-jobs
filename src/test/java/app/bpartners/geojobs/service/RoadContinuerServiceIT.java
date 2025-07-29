package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonValidator;
import app.bpartners.geojobs.endpoint.rest.postprocessing.continuer.LatLonLinesContinuer;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.LatLonPolygon;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.file.hash.FileHashAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.MockedStatic;

public class RoadContinuerServiceIT extends FacadeIT {

  private BucketComponent bucketComponent;
  private RoadContinuerService subject;

  @BeforeEach
  public void setUp() {
    bucketComponent = mock(BucketComponent.class);
    var mapper = new ObjectMapper();
    var geoJsonValidator = new GeoJsonValidator(mapper);
    subject = new RoadContinuerService(bucketComponent, geoJsonValidator);
  }

  @Test
  void test_success_continuation_with_empty_features() throws Exception {
    var input =
        """
            {
              "features" : [ ],
              "type" : "FeatureCollection"
            }
        """;

    File fileMock = File.createTempFile("geojson-test-", ".geojson");
    Files.writeString(fileMock.toPath(), input, StandardCharsets.UTF_8);

    var polygon =
        new GeometryFactory()
            .createPolygon(
                new Coordinate[] {
                  new Coordinate(0, 0),
                  new Coordinate(1, 0),
                  new Coordinate(1, 1),
                  new Coordinate(0, 1),
                  new Coordinate(0, 0)
                });
    var latLonPolygon = new LatLonPolygon(polygon);
    String mockedURL = "https://mocked/continued.geojson";

    when(bucketComponent.upload(any(File.class), anyString()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "DummyValue"));

    when(bucketComponent.presign(anyString())).thenReturn(mockedURL);

    try (MockedStatic<RoadContinuerService> staticMock =
        mockStatic(RoadContinuerService.class, CALLS_REAL_METHODS)) {
      staticMock.when(() -> RoadContinuerService.getGeoJsonFromString(input)).thenReturn(fileMock);

      var continuerMock = mock(LatLonLinesContinuer.class);
      when(continuerMock.apply(any(File.class))).thenReturn(Set.of(latLonPolygon));

      var result = subject.continueRoute(input, 20, 1_024);

      assertNotNull(result);
      assertEquals(mockedURL, result.get("url"));

      verify(bucketComponent).upload(any(File.class), anyString());
      verify(bucketComponent).presign(anyString());
    }
  }

  @Test
  void test_continuation_with_ambohijatovo_geojson_content()
      throws URISyntaxException, IOException {
    var resourceUrl = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");
    assertNotNull(resourceUrl);

    var geojsonFile = new File(resourceUrl.toURI());
    String geojsonContent = Files.readString(geojsonFile.toPath(), StandardCharsets.UTF_8);
    String mockedURL = "https://mocked/ambohijatovo-continued.geojson";

    when(bucketComponent.upload(any(File.class), anyString()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "DummyValue"));

    when(bucketComponent.presign(anyString())).thenReturn(mockedURL);

    var result = subject.continueRoute(geojsonContent, 17, 1_024);

    assertNotNull(result);
    assertEquals(mockedURL, result.get("url"));

    verify(bucketComponent).upload(any(File.class), anyString());
    verify(bucketComponent).presign(anyString());
  }
}
