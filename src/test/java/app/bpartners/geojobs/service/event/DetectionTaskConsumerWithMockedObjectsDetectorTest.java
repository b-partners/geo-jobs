package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.ParcelContent;
import app.bpartners.geojobs.repository.model.detection.DetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.MockedObjectsDetector;
import java.util.List;
import org.junit.jupiter.api.Test;

class DetectionTaskConsumerWithMockedObjectsDetectorTest {

  @Test
  void can_consume_with_no_error() {
    var subject = new DetectionTaskConsumer(mock(), new MockedObjectsDetector(5_000, 0), mock());
    subject.accept(
        new DetectionTask()
            .toBuilder()
                .parcels(
                    List.of(
                        new Parcel()
                            .toBuilder()
                                .parcelContent(
                                    ParcelContent.builder()
                                        .tiles(
                                            List.of(
                                                Tile.builder()
                                                    .coordinates(
                                                        new TileCoordinates().z(20).x(0).y(0))
                                                    .build()))
                                        .build())
                                .build()))
                .build());
  }

  @Test
  void can_consume_with_some_errors() {
    var subject = new DetectionTaskConsumer(mock(), new MockedObjectsDetector(2_000, 50), mock());

    try {
      for (int i = 0; i < 10; i++) {
        subject.accept(
            new DetectionTask()
                .toBuilder()
                    .parcels(
                        List.of(
                            new Parcel()
                                .toBuilder()
                                    .parcelContent(
                                        ParcelContent.builder().tiles(List.of(new Tile())).build())
                                    .build()))
                    .build());
      }
    } catch (Exception e) {
      return;
    }
    fail();
  }
}
