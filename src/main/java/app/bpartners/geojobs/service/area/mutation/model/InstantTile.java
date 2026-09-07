package app.bpartners.geojobs.service.area.mutation.model;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.net.URL;
import java.time.Instant;
import java.util.List;

/** A parcel's imagery at a single date (millésime), with the API used to fetch it. */
public record InstantTile(
    Instant date, List<FeatureWithDelimitation> parcelDelimitations, ImageSource imageSource) {

  /** The image server call that produced this tile's imagery. */
  public record ImageSource(URL apiUrl, GeoServerParameter geoServerParameter) {}
}
