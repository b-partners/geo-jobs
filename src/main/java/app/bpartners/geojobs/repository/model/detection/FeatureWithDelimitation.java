package app.bpartners.geojobs.repository.model.detection;

import app.bpartners.geojobs.repository.model.Feature;
import java.util.List;

public record FeatureWithDelimitation(Feature feature, List<Feature> delimitations) {}
