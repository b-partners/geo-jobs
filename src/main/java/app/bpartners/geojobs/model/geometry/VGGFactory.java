package app.bpartners.geojobs.model.geometry;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.convert.converter.Converter;

public class VGGFactory implements Converter<Set<Polygon>, VGG> {
  @Override
  public VGG convert(Set<Polygon> polygons) {
    var vgg = new VGG();
    for (Polygon p : polygons) {
      var metadata = (HashMap) p.getUserData();
      var key = metadata.get("filename").toString();
      var label = metadata.get("label").toString();
      var confidence = Double.parseDouble(metadata.get("confidence").toString());
      Map<String, VGG.Annotation.Region> newRegions = new HashMap<>();
      newRegions.put(String.valueOf(Instant.now().getNano()), toVGGRegion(label, confidence, p));
      if (vgg.containsKey(key)) {
        var annotation = vgg.get(key);
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
