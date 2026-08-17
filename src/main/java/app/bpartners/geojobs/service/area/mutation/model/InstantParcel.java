package app.bpartners.geojobs.service.area.mutation.model;

import app.bpartners.geojobs.repository.model.detection.FeatureWithDelimitation;
import java.time.Instant;
import java.util.List;

public record InstantParcel(Instant date, List<FeatureWithDelimitation> parcelDelimitations) {}
