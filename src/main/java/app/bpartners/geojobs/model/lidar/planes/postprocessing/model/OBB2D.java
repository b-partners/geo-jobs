package app.bpartners.geojobs.model.lidar.planes.postprocessing.model;

import app.bpartners.geojobs.model.lidar.LasPointGeometry;
import lombok.Builder;

@Builder
public record OBB2D(
    LasPointGeometry center, double area, double angle, double width, double height) {}
