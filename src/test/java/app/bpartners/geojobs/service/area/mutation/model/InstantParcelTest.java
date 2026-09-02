package app.bpartners.geojobs.service.area.mutation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstantParcelTest {
  @Test
  void exposes_date_and_parcel_delimitations() {
    var date = Instant.parse("2024-06-01T00:00:00Z");
    var delimitations =
        List.of(new FeatureWithDelimitation(Feature.builder().id("feature-1").build(), List.of()));

    var actual = new InstantParcel(date, delimitations);

    assertEquals(date, actual.date());
    assertEquals(delimitations, actual.parcelDelimitations());
  }
}
