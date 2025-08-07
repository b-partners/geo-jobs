package app.bpartners.geojobs.endpoint.rest.postprocessing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class GeoJsonValidatorIT {
  private final MultipartFile mockMultipartFile = mock(MultipartFile.class);
  private GeoJsonValidator subject;

  @BeforeEach
  void setUp() {
    var mapper = new ObjectMapper();
    subject = new GeoJsonValidator(mapper);
  }

  @Test
  void testIsLikelyGeoJson_withValidMimeTypeAndExtension() {
    when(mockMultipartFile.getOriginalFilename()).thenReturn("data.geojson");
    when(mockMultipartFile.getContentType()).thenReturn("application/geo+json");

    boolean result = subject.isLikelyGeoJson(mockMultipartFile);

    assertTrue(result);
  }

  @Test
  void test_non_geojson_file() {
    when(mockMultipartFile.getOriginalFilename()).thenReturn("dummy.txt");
    when(mockMultipartFile.getContentType()).thenReturn("text/plain");

    boolean result = subject.isLikelyGeoJson(mockMultipartFile);

    assertFalse(result);
  }
}
