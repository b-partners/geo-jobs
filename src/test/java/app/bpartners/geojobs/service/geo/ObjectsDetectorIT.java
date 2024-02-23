package app.bpartners.geojobs.service.geo;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.ObjectsDetector;
import java.io.File;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ObjectsDetectorIT extends FacadeIT {
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
  @Autowired ObjectsDetector objectsDetector;

  @Test
  public void process_detection_ok() {
    var actual = objectsDetector.apply(detectionTask());

    assertNotNull(actual);
    assertNotNull(actual.getRstImageUrl());
    assertNotNull(actual.getSrcImageUrl());
    assertNotNull(actual.getRstRaw());
  }

  public DetectionTask detectionTask() {
    when(bucketComponent.download(any())).thenReturn(new File(FILE_NAME));

    return DetectionTask.builder()
        .id(String.valueOf(randomUUID()))
        .jobId(String.valueOf(randomUUID()))
        .submissionInstant(Instant.now())
        .parcels(
            List.of(
                Parcel.builder()
                    .id(randomUUID().toString())
                    .parcelContent(
                        ParcelContent.builder()
                            .id(randomUUID().toString())
                            .tiles(
                                List.of(
                                    Tile.builder()
                                        .id(randomUUID().toString())
                                        .bucketPath(randomUUID().toString())
                                        .build()))
                            .build())
                    .build()))
        .build();
  }
}
