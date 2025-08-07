package app.bpartners.geojobs.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.rest.postprocessing.GeoJsonValidator;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.file.hash.FileHashAlgorithm;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

public class RoadContinuerServiceIT extends FacadeIT {

  private final BucketComponent bucketComponent = mock(BucketComponent.class);
  @Autowired private GeoJsonValidator geoJsonValidator;
  private RoadContinuerService subject;

  public static MultipartFile convertFileToMultipartFile(File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      return new MockMultipartFile("geojson-file", file.getName(), "application/geo+json", fis);
    }
  }

  @BeforeEach
  public void setUp() {
    subject = new RoadContinuerService(bucketComponent, geoJsonValidator);
  }

  @Test
  void test_continuation_with_ambohijatovo_geojson_content()
      throws URISyntaxException, IOException {
    var resourceUrl = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");
    assertNotNull(resourceUrl);
    var geoJSONMultipartFile = convertFileToMultipartFile(new File(resourceUrl.toURI()));
    String mockedURL = "https://mocked/ambohijatovo-continued.geojson";

    when(bucketComponent.upload(any(File.class), anyString()))
        .thenReturn(new FileHash(FileHashAlgorithm.SHA256, "DummyValue"));
    when(bucketComponent.presign(anyString())).thenReturn(mockedURL);

    var result = subject.continueRoute(geoJSONMultipartFile, 17, 1_024);

    assertNotNull(result);
    assertEquals(mockedURL, result.get("url"));

    verify(bucketComponent).upload(any(File.class), anyString());
    verify(bucketComponent).presign(anyString());
  }
}
