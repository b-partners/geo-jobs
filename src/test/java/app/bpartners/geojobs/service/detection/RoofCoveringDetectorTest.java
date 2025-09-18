package app.bpartners.geojobs.service.detection;

import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.ROOF_ARDOISE;
import static app.bpartners.geojobs.repository.model.detection.RoofCoveringType.ROOF_TUILES;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.CustomBucketComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class RoofCoveringDetectorTest {
  ObjectMapper objectMapperMock = mock();
  RestTemplate restTemplateMock = mock();
  CustomBucketComponent bucketComponentMock = mock();
  RoofCoveringDetector subject =
      new RoofCoveringDetector(
          objectMapperMock,
          restTemplateMock,
          "https://detection.api",
          "api-token",
          bucketComponentMock);

  @Test
  void apply_ok() {
    var expected = getRoofCoveringDetectionBody();
    when(restTemplateMock.postForEntity(
            any(String.class), any(), eq(RoofCoveringDetectionResponse.class)))
        .thenReturn(new ResponseEntity<>(expected, HttpStatus.OK));

    var actual = subject.apply("image64", "mask64");

    assertEquals(expected, actual);
  }

  private RoofCoveringDetectionResponse getRoofCoveringDetectionBody() {
    return new RoofCoveringDetectionResponse(
        new RoofCovering(ROOF_ARDOISE, 1343), new RoofCovering(ROOF_TUILES, 1000));
  }
}
