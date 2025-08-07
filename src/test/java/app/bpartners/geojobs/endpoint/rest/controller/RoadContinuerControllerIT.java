package app.bpartners.geojobs.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;

class RoadContinuerControllerIT extends FacadeIT {
  private static final String preSignedUrl =
      "https://mocked-s3-url.com/dummy-continued-roads.geojson";

  @Autowired private RoadContinuerController subject;

  public static MockMultipartFile convertFileToMultipartFile(File file) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      return new MockMultipartFile("geojson-file", file.getName(), "application/geo+json", fis);
    }
  }

  @Test
  void test_continuation_of_quai_de_bourbon() throws Exception {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);

    var uploadedGeoJSON = convertFileToMultipartFile(new File(resource.toURI()));
    assertNotNull(uploadedGeoJSON);
    int zoom = 17;
    int imageSize = 1_024;

    var actual = subject.roadContinuer(uploadedGeoJSON, zoom, imageSize);
    assertNotNull(actual);
    assertEquals(preSignedUrl, actual.get("url"));
  }

  @Test
  void test_continuation_of_non_valid_file() throws Exception {
    var resource = getClass().getResource("/shape/dummy.shape");
    assertNotNull(resource);

    var uploadedGeoJSON = convertFileToMultipartFile(new File(resource.toURI()));
    assertNotNull(uploadedGeoJSON);
    var zoom = 20;
    var imageSize = 1_024;

    assertThrows(
        IllegalArgumentException.class,
        () -> subject.roadContinuer(uploadedGeoJSON, zoom, imageSize));
  }

  @TestConfiguration
  static class MockConfig {
    @Bean
    public BucketComponent bucketComponent() {
      BucketComponent mock = mock(BucketComponent.class);
      when(mock.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
      when(mock.presign(anyString())).thenReturn(preSignedUrl);
      return mock;
    }
  }
}
