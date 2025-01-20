package app.bpartners.geojobs.model.geometry;

import app.bpartners.geojobs.model.geometry.polygon.Feature;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.convert.converter.Converter;

public class VGGFactory implements Converter<List<Feature>, VGG> {
  @Override
  public VGG convert(List<Feature> features) {
    var vgg = new VGG();
    for (Feature f : features) {
      var key = f.filename();
      Map<String, VGG.Annotation.Region> newRegions = new HashMap<>();
      newRegions.put(
          String.valueOf(Instant.now().getNano()),
          toVGGRegion(f.label(), f.confidence(), f.geometry()));
      if (vgg.containsKey(key)) {
        var annotation = vgg.get(f.filename());
        newRegions.putAll(annotation.getRegions());
        annotation.setRegions(newRegions);
        vgg.put(key, annotation);
      }
      var annotation = VGG.Annotation.builder().filename(key).regions(newRegions).build();
      vgg.putIfAbsent(key, annotation);
    }
    return vgg;
  }

  private VGG.Annotation.Region toVGGRegion(String label, double confidence, Polygon geometry) {
    List<Double> allX = Arrays.stream(geometry.getCoordinates()).map(coor -> coor.x).toList();
    List<Double> allY = Arrays.stream(geometry.getCoordinates()).map(coor -> coor.y).toList();
    var name = "Polygon";
    return VGG.Annotation.Region.builder()
        .regionAttribute(
            VGG.Annotation.Region.RegionAttribute.builder()
                .label(label)
                .confidence(confidence)
                .build())
        .shapeAttribute(
            VGG.Annotation.Region.ShapeAttribute.builder()
                .name(name)
                .allPointsX(allX)
                .allPointsY(allY)
                .build())
        .build();
  }
}
