package app.bpartners.geojobs.model.geometry.polygon;

import org.locationtech.jts.geom.Polygon;

public record Feature(String filename, String label, double confidence, Polygon geometry) {}
