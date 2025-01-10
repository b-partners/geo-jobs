package app.bpartners.geojobs.model.geometry.polygon;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;

public record FeatureCollection(List<Feature> features) {

  public static FeatureCollection from(VGG vggAnnotations){
    var keys = vggAnnotations.keySet();
    List<Feature> features = new ArrayList<>();
    for (String key : keys){
      var regions = vggAnnotations.get(key).getRegions().values();
      for (VGG.Annotation.Region region : regions) {
        var label = region.getRegionAttribute().getLabel();
        var confidence = region.getRegionAttribute().getConfidence();
        var allX = region.getShapeAttribute().getAllPointsX();
        var allY = region.getShapeAttribute().getAllPointsY();
        var geometry = parse(allX, allY);
        features.add(new Feature(key, label, confidence, geometry));
      }
    }
    return new FeatureCollection(features);
  }

  private static Polygon parse(List<Double> allX, List<Double> allY){
    var size = allX.size();
    var coordinates = new Coordinate[size];
    for (int i = 0; i < size; i++) {
      coordinates[i] = new Coordinate(allX.get(i), allY.get(i));
    }
    return (Polygon) geometryFactory.createPolygon(coordinates).buffer(0.1);
  }

  public record Feature(String filename, String label, double confidence, Polygon geometry){}
}
