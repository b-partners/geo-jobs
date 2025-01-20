package app.bpartners.geojobs.model.geometry.polygon;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static java.util.Collections.min;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.model.geometry.IntXY;
import java.util.List;
import java.util.function.Supplier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

public class FeatureListOffsetRestorer implements Supplier<List<Feature>> {

  private final List<Feature> features;

  public FeatureListOffsetRestorer(List<Feature> features) {
    var minTileXY = min(features.stream().map(Feature::tileCoordinate).collect(toSet()));
    this.features = restoreOffset(features, minTileXY);
  }

  @Override
  public List<Feature> get() {
    return features;
  }

  private List<Feature> restoreOffset(List<Feature> features, IntXY offset) {
    return features.stream().map(feature -> restoreOffset(feature, offset)).toList();
  }

  private Feature restoreOffset(Feature f, IntXY minTileXY) {
    return f.toBuilder()
        .geometry(restoreOffset(f.geometry(), f.tileCoordinate(), minTileXY, f.imageResolution()))
        .build();
  }

  private Polygon restoreOffset(Polygon p, IntXY tileXY, IntXY minTileXY, IntXY imageResolution) {
    var oldCoordinates = p.getCoordinates();
    var newCoordinates = new Coordinate[oldCoordinates.length];
    for (int i = 0; i < newCoordinates.length; i++) {
      var oldCoordinateI = oldCoordinates[i];
      int xOffset = (tileXY.x() - minTileXY.x()) * imageResolution.x();
      int yOffset = (tileXY.y() - minTileXY.y()) * imageResolution.y();
      newCoordinates[i] = new Coordinate(oldCoordinateI.x - xOffset, oldCoordinateI.y - yOffset);
    }

    return geometryFactory.createPolygon(newCoordinates);
  }
}
