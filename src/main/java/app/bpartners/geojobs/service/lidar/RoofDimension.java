package app.bpartners.geojobs.service.lidar;

import org.locationtech.jts.geom.Polygon;

public record RoofDimension(Polygon polygon, double slope, double height) {
}
