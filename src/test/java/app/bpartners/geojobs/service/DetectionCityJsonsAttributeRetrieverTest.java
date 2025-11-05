package app.bpartners.geojobs.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.repository.model.cityjson.CityJSON;
import app.bpartners.geojobs.repository.model.detection.Detection;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionCityJsonsAttributeRetrieverTest {
  private static final BucketComponent bucketComponentMock = mock();
  private static final DetectionCityJsonsAttributeRetriever subject =
      new DetectionCityJsonsAttributeRetriever(bucketComponentMock);

  @Test
  void retrieve_attribute_ok() {
    var detection =
        Detection.builder()
            .id(randomUUID().toString())
            .cityJsons(List.of(CityJSON.builder().s3FileKey(randomUUID().toString()).build()))
            .build();

    var expectedUrl = "https://dummy.com";
    var expected =
        List.of(new app.bpartners.geojobs.endpoint.rest.model.CityJSON().url(expectedUrl));
    when(bucketComponentMock.presign(any())).thenReturn(expectedUrl);

    var actual = subject.apply(detection);

    assertEquals(expected, actual);
  }
}
