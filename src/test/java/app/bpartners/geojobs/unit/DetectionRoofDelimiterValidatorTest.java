package app.bpartners.geojobs.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.model.exception.BadRequestException;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.service.DetectionRoofDelimiterValidator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionRoofDelimiterValidatorTest {

  DetectionRoofDelimiterValidator subject = new DetectionRoofDelimiterValidator();

  @Test
  void throws_bad_request_on_image_not_present() {
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getImageFileKey()).thenReturn(null);
    when(detectionMock.getPolygonRoofDelimitation()).thenReturn(null);
    when(detectionMock.isSucceeded()).thenReturn(false);

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(detectionMock));

    assertEquals(
        String.format(
            "Detection.image is mandatory before configuring root delimiter"
                + " otherwise actual detection.id=%s does not have image. ",
            detectionId),
        actual.getMessage());
  }

  @Test
  void throws_bad_request_on_roof_delimiter_existing() {
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getImageFileKey()).thenReturn(randomUUID().toString());
    when(detectionMock.getPolygonRoofDelimitation()).thenReturn(List.of(new ArrayList<>()));
    when(detectionMock.isSucceeded()).thenReturn(false);

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(detectionMock));

    assertEquals(
        String.format("Detection.id=%s roofDelimiter.polygon already set to [[]]. ", detectionId),
        actual.getMessage());
  }

  @Test
  void throws_bad_request_on_detection_succeeded() {
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getImageFileKey()).thenReturn(randomUUID().toString());
    when(detectionMock.getPolygonRoofDelimitation()).thenReturn(null);
    when(detectionMock.isSucceeded()).thenReturn(true);

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(detectionMock));

    assertEquals(
        String.format(
            "Detection.id=%s is already succeeded and can't be launched anymore.", detectionId),
        actual.getMessage());
  }

  @Test
  void do_nothing_on_valid_detection() {
    var detectionId = randomUUID().toString();
    var detectionMock = mock(Detection.class);
    when(detectionMock.getId()).thenReturn(detectionId);
    when(detectionMock.getImageFileKey()).thenReturn(randomUUID().toString());
    when(detectionMock.getPolygonRoofDelimitation()).thenReturn(null);
    when(detectionMock.isSucceeded()).thenReturn(false);

    assertDoesNotThrow(() -> subject.accept(detectionMock));
  }
}
