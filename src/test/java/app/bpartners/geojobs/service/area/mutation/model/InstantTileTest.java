package app.bpartners.geojobs.service.area.mutation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.Feature;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstantTileTest {
  @Test
  void exposes_date_parcel_delimitations_and_image_source() throws MalformedURLException {
    var date = Instant.parse("2024-06-01T00:00:00Z");
    var delimitations =
        List.of(new FeatureWithDelimitation(Feature.builder().id("feature-1").build(), List.of()));
    var apiUrl = new URL("http://geoserver.test/wms");
    var geoServerParameter = new GeoServerParameter();
    var imageSource = new InstantTile.ImageSource(apiUrl, geoServerParameter);

    var actual = new InstantTile(date, delimitations, imageSource);

    assertEquals(date, actual.date());
    assertEquals(delimitations, actual.parcelDelimitations());
    assertEquals(imageSource, actual.imageSource());
    assertEquals(apiUrl, actual.imageSource().apiUrl());
    assertEquals(geoServerParameter, actual.imageSource().geoServerParameter());
  }
}
