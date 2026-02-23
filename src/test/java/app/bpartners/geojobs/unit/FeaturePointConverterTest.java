package app.bpartners.geojobs.unit;

import static app.bpartners.geojobs.model.DelimitationObjectType.PARCEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.service.FeaturePointConverter;
import org.junit.jupiter.api.Test;

class FeaturePointConverterTest {
  FeaturePointConverter subject = new FeaturePointConverter(mock());

  @Test
  void throw_not_implemented_when_delimitation_object_not_BUILDING() {
    var actual = assertThrows(NotImplementedException.class, () -> subject.apply(mock(), PARCEL));

    assertEquals(
        "Unable to convert address to Feature for delimitationObjectType PARCEL",
        actual.getMessage());
  }
}
