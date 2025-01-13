package app.bpartners.geojobs.model.geometry.feature;

import org.locationtech.jts.geom.Polygon;

public record Feature(String filename, String label, double confidence, Polygon geometry) {}
