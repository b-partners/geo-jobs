package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.detection.Detection;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

class DetectionImageAttributeRetrieverTest {
  BucketComponent bucketComponentMock = mock();
  DetectionImageAttributeRetriever subject =
      new DetectionImageAttributeRetriever(bucketComponentMock);

  @Test
  void retrieve_image_from_image_file_key() {
    var detectionMock = mock(Detection.class);
    var imageFileKey = randomUUID().toString();
    var presignUrl = "http://localhost/" + randomUUID();
    when(detectionMock.getImageFileKey()).thenReturn(imageFileKey);
    when(bucketComponentMock.presign(imageFileKey)).thenReturn(presignUrl);

    var actual = subject.apply(detectionMock);

    assertEquals(presignUrl, actual);
  }

  @Test
  void retrieve_image_from_detection_polygon_geo_json_condition() {
    var detectionMock = mock(Detection.class);
    var presignUrl = "http://localhost/" + randomUUID();
    var detectionIdentifier = randomUUID().toString();

    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getPolygonGeoJsonZone()).thenReturn(mock(Feature.class));
    when(detectionMock.getImageFileKey()).thenReturn(null);
    when(bucketComponentMock.presign("zone_images/" + detectionIdentifier + ".jpg"))
        .thenReturn(presignUrl);

    var actual = subject.apply(detectionMock);

    assertEquals(presignUrl, actual);
  }

  @Test
  void return_null_when_exception_occurs_on_presigning_url() {
    var detectionMock = mock(Detection.class);
    var detectionIdentifier = randomUUID().toString();

    when(detectionMock.getId()).thenReturn(detectionIdentifier);
    when(detectionMock.getPolygonGeoJsonZone()).thenReturn(mock(Feature.class));
    when(detectionMock.getImageFileKey()).thenReturn(null);
    doThrow(AwsServiceException.class)
        .when(bucketComponentMock)
        .presign("zone_images/" + detectionIdentifier + ".jpg");

    var actual = subject.apply(detectionMock);

    assertNull(actual);
  }

  @Test
  void retrieve_null_when_both_file_key_and_property_null() {
    var detectionMock = mock(Detection.class);
    when(detectionMock.getVggFileKey()).thenReturn(null);
    when(detectionMock.getPolygonGeoJsonZone()).thenReturn(null);

    var actual = subject.apply(detectionMock);

    verify(bucketComponentMock, never()).presign(any());
    assertNull(actual);
  }
}
