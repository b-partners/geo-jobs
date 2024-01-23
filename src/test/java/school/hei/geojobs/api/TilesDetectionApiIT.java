package school.hei.geojobs.api;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.geojobs.conf.FacadeIT;
import school.hei.geojobs.file.BucketComponent;
import school.hei.geojobs.repository.model.Tile;
import school.hei.geojobs.repository.model.ZoneDetectionTask;
import school.hei.geojobs.service.geo.TilesDetectionApi;
import school.hei.geojobs.service.geo.response.DetectionResponse;

public class TilesDetectionApiIT extends FacadeIT {
  private static final String FILE_NAME =
      "src"
          + File.separator
          + "test"
          + File.separator
          + "resources"
          + File.separator
          + "mockData"
          + File.separator
          + "image-to-detect.jpg";
  @MockBean BucketComponent bucketComponent;
  @Autowired TilesDetectionApi tilesDetectionApi;

  @Test
  public void process_detection_ok() {
    DetectionResponse actual = tilesDetectionApi.detect(zoneDetectionTask());

    assertNotNull(actual);
    assertNotNull(actual.getRstImageUrl());
    assertNotNull(actual.getSrcImageUrl());
    assertNotNull(actual.getRstRaw());
  }

  public ZoneDetectionTask zoneDetectionTask() {
    when(bucketComponent.download(any())).thenReturn(new File(FILE_NAME));

    return ZoneDetectionTask.builder()
        .id(String.valueOf(randomUUID()))
        .jobId(String.valueOf(randomUUID()))
        .submissionInstant(Instant.now())
        .tile(
            Tile.builder()
                .id(String.valueOf(randomUUID()))
                .bucketPath(String.valueOf(randomUUID()))
                .build())
        .build();
  }
}
