package app.bpartners.geojobs.service.area.mutation.model;

import app.bpartners.geojobs.endpoint.rest.model.GeoServerParameter;
import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.io.File;
import java.net.URL;
import java.util.List;

/** Information needed to detect a mutation between two ortho imagery dates of a parcel. */
public record MutationContext(
    List<FeatureWithDelimitation> parcelDelimitations,
    File maskImageFile,
    URL geoServerUrl,
    GeoServerParameter geoServerParameter) {}
