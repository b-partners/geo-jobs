package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.repository.model.detection.DetectableType.LINE;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.ROOF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectConfigurationMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.DetectableObjectTypeMapper;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.file.bucket.BucketConf;
import app.bpartners.geojobs.repository.model.detection.DetectableObjectConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DetectableObjectConfigufrationMapperTest {
  BucketConf bucketConf = mock();
  DetectableObjectConfigurationMapper subject =
      new DetectableObjectConfigurationMapper(new DetectableObjectTypeMapper(), bucketConf);

  @Test
  void to_rest_ok() {
    var dummyBucket = "dummyBucket";
    var expected1 =
        new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
            .confidence(BigDecimal.valueOf(0.5))
            .type(DetectableObjectType.ROOF)
            .bucketStorageName(dummyBucket);
    var expected2 =
        new app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration()
            .confidence(BigDecimal.valueOf(1))
            .type(DetectableObjectType.LINE)
            .bucketStorageName(dummyBucket);

    var actual1 =
        subject.toRest(
            DetectableObjectConfiguration.builder()
                .confidence(0.5)
                .objectType(ROOF)
                .bucketStorageName(dummyBucket)
                .build());
    var actual2 =
        subject.toRest(
            DetectableObjectConfiguration.builder()
                .confidence(null)
                .objectType(LINE)
                .bucketStorageName(dummyBucket)
                .build());

    assertEquals(expected1, actual1);
    assertEquals(expected2, actual2);
  }
}
