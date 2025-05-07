package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade.*;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VGGFactory implements Converter<Set<Polygon>, VGG> {
  private final FeatureMapper featureMapper;

  @Override
  public VGG convert(Set<Polygon> polygons) {
    var vgg = new VGG();
    for (Polygon p : polygons) {
      var metadata = (HashMap) p.getUserData();
      var key = metadata.get("filename").toString();
      var label = metadata.get("label").toString();
      var confidence = Double.parseDouble(metadata.get("confidence").toString());
      Map<String, VGG.Annotation.Region> newRegions = new HashMap<>();
      newRegions.put(
          String.valueOf(Instant.now().getNano()), toVGGRegion(label, confidence, null, p));
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

  public VGG from(Polygon roofGeometry, List<DetectedTile> detectedTiles) {
    var vgg = new VGG();
    for (var detectedTile : detectedTiles) {
      var rateComputer = new AreaRateComputerFacade(roofGeometry, detectedTile);
      var detectedObjects = detectedTile.getDetectedObjects();
      var tile = detectedTile.getTile().getCoordinates();
      var key = String.format("%s_%s_%s.jpg", tile.getZ(), tile.getX(), tile.getY());

      var usureRate = rateComputer.getUsureAreaRate();
      var humiditeRate = rateComputer.getHumidityAreaRate();
      var moisissureRate = rateComputer.getMoisissureAreaRate();
      var globalRateValue = rateComputer.getGlobalRate();
      var globalRateType = rateComputer.getRate();

      Map<String, VGG.Annotation.Region> regions = new HashMap<>();
      for (var object : detectedObjects) {
        var label = object.getDetectableObjectType();
        var confidence = object.getComputedConfidence();
        var rate = rateComputer.compute(label);
        var polygon = featureMapper.toDomain(object.getFeature());
        regions.put(
            String.valueOf(System.nanoTime()),
            toVGGRegion(label.name(), confidence, rate, polygon));
      }

      var properties = new HashMap<String, Object>();
      properties.put("usure_rate", usureRate);
      properties.put("humidite_rate", humiditeRate);
      properties.put("moisissure_rate", moisissureRate);
      properties.put("global_rate_value", globalRateValue);
      properties.put("global_rate_type", globalRateType);

      var annotation =
          VGG.Annotation.builder().filename(key).properties(properties).regions(regions).build();
      vgg.putIfAbsent(key, annotation);
    }
    return vgg;
  }

  private VGG.Annotation.Region toVGGRegion(
      String label, Double confidence, Double rate, Polygon geometry) {
    List<Double> allX = Arrays.stream(geometry.getCoordinates()).map(coor -> coor.x).toList();
    List<Double> allY = Arrays.stream(geometry.getCoordinates()).map(coor -> coor.y).toList();
    var name = "Polygon";
    return VGG.Annotation.Region.builder()
        .regionAttribute(
            VGG.Annotation.Region.RegionAttribute.builder()
                .label(label)
                .confidence(confidence)
                .rate_in_percent(rate)
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
