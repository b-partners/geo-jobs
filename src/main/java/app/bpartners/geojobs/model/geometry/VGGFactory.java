package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade.*;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TiledPolygon;
import app.bpartners.geojobs.endpoint.rest.postprocessing.model.TilingConf;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.NotImplementedException;
import app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.PolygonArea;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class VGGFactory implements Converter<Set<Polygon>, VGG> {
  private static final int DEFAULT_IMG_SIZE = 1024;
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

  public Map<Feature, VGG> from(
      List<TiledPixelPolygon> tiledPixelPolygons, MultiPolygon roofLatLonMultiPolygon) {
    Map<Feature, List<TiledPixelPolygon>> tiledPixelPolygonFilteredByPoint =
        tiledPixelPolygons.stream().collect(Collectors.groupingBy(TiledPixelPolygon::point));
    var vggMap = new HashMap<Feature, VGG>();

    tiledPixelPolygonFilteredByPoint.forEach(
        (featurePoint, tiledPolygons) -> {
          var vgg = new VGG();
          int minTileX =
              tiledPolygons.stream().mapToInt(TiledPixelPolygon::tileX).min().orElseThrow();
          int minTileY =
              tiledPolygons.stream().mapToInt(TiledPixelPolygon::tileY).min().orElseThrow();
          tiledPolygons.forEach(
              tiledPolygon -> {
                var key =
                    String.format(
                        "%s_%s_%s_%s.jpg",
                        randomUUID(),
                        tiledPolygon.zoom(),
                        tiledPolygon.tileX(),
                        tiledPolygon.tileY());
                List<PolygonObjectType> originalPolygonObjectTypes = tiledPolygon.polygons();
                var projectedPolygonObjectTypes =
                    originalPolygonObjectTypes.stream()
                        .map(
                            polygonObjectType -> {
                              var projectedPolygonsToCompositeImage =
                                  projectPolygonsToCompositeImage(
                                      tiledPolygon.tileX(),
                                      tiledPolygon.tileY(),
                                      minTileX,
                                      minTileY,
                                      DEFAULT_IMG_SIZE,
                                      polygonObjectType.polygon());
                              return new PolygonObjectType(
                                  projectedPolygonsToCompositeImage,
                                  polygonObjectType.objectType());
                            })
                        .toList();

                Map<String, VGG.Annotation.Region> regions = new HashMap<>();

                projectedPolygonObjectTypes.forEach(
                    polygonObjectType -> {
                      var detectedObjectPolygon = polygonObjectType.polygon();
                      var label = polygonObjectType.objectType();
                      var rate =
                          format(
                              (polygonObjectType.polygon().getArea()
                                      / roofLatLonMultiPolygon.getArea())
                                  * 100);
                      regions.put(
                          String.valueOf(System.nanoTime()),
                          toVGGRegion(label.name(), null, rate, detectedObjectPolygon));
                    });

                var properties =
                    computeProperties(roofLatLonMultiPolygon, originalPolygonObjectTypes);
                var annotation =
                    VGG.Annotation.builder()
                        .filename(key)
                        .properties(properties)
                        .regions(regions)
                        .build();
                vgg.putIfAbsent(key, annotation);
              });

          vggMap.put(featurePoint, vgg);
        });
    return vggMap;
  }

  private HashMap<String, Object> computeProperties(
      Geometry roofLatLonMultiPolygon, List<PolygonObjectType> originalPolygonObjectTypes) {
    var rateComputer =
        new AreaRateComputerFacade(roofLatLonMultiPolygon, originalPolygonObjectTypes);
    var usureRate = rateComputer.getUsureAreaRate();
    var humiditeRate = rateComputer.getHumidityAreaRate();
    var moisissureRate = rateComputer.getMoisissureAreaRate();
    var globalRateValue = rateComputer.getGlobalRate();
    var globalRateType = rateComputer.getRate();

    var properties = new HashMap<String, Object>();

    properties.put("roof_area_in_m2", computeRoofArea(roofLatLonMultiPolygon));
    properties.put("usure_rate", usureRate);
    properties.put("humidite_rate", humiditeRate);
    properties.put("moisissure_rate", moisissureRate);
    properties.put("global_rate_value", globalRateValue);
    properties.put("global_rate_type", globalRateType);

    return properties;
  }

  private Polygon projectPolygonsToCompositeImage(
      Integer tileX,
      Integer tileY,
      int minTileX,
      int minTileY,
      int tileSize,
      Polygon originalPolygon) {
    int offsetX = (tileX - minTileX) * tileSize;
    int offsetY = (tileY - minTileY) * tileSize;
    return translatePolygon(originalPolygon, offsetX, offsetY);
  }

  private Polygon translatePolygon(Polygon polygon, int offsetX, int offsetY) {
    Coordinate[] coords = polygon.getCoordinates();
    Coordinate[] newCoords =
        Arrays.stream(coords)
            .map(coord -> new Coordinate(coord.x + offsetX, coord.y + offsetY))
            .toArray(Coordinate[]::new);

    return polygon.getFactory().createPolygon(newCoords);
  }

  public VGG from(Polygon roofGeometry, List<DetectedTile> detectedTiles) {
    var vgg = new VGG();
    var originTile = detectedTiles.getFirst().getTile();
    var originTileCoords =
        new IntXY(originTile.getCoordinates().getX(), originTile.getCoordinates().getY());
    var tilingConf =
        new TilingConf(originTile.getCoordinates().getZ(), originTile.getSize().getHeight());
    var roofGeometryAsTile =
        new TiledPolygon(roofGeometry, null, originTileCoords, tilingConf)
            .latLonPolygon()
            .polygon();
    System.out.println(roofGeometryAsTile);
    var roofAreaInM2 = computeRoofArea(roofGeometryAsTile);
    for (var detectedTile : detectedTiles) {
      var rateComputer = new AreaRateComputerFacade(roofGeometry, detectedTile);
      var detectedObjects = detectedTile.getDetectedObjects();
      var tile = detectedTile.getTile().getCoordinates();
      var key =
          String.format("%s_%s_%s_%s.jpg", randomUUID(), tile.getZ(), tile.getX(), tile.getY());

      var usureRate = rateComputer.getUsureAreaRate();
      var humiditeRate = rateComputer.getHumidityAreaRate();
      var moisissureRate = rateComputer.getMoisissureAreaRate();
      var globalRateValue = rateComputer.getGlobalRate();
      var globalRateType = rateComputer.getRate();

      Map<String, VGG.Annotation.Region> regions = new HashMap<>();
      for (var object : detectedObjects) {
        var label = object.getDetectableObjectType();
        var confidence = object.getComputedConfidence();
        var polygon = featureMapper.toDomain(object.getFeature());
        var rate = format((polygon.getArea() / roofGeometry.getArea()) * 100);
        regions.put(
            String.valueOf(System.nanoTime()),
            toVGGRegion(label.name(), confidence, rate, polygon));
      }

      var properties = new HashMap<String, Object>();
      properties.put("roof_area_in_m2", roofAreaInM2);
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

  // Mostly ChatGPT generated
  @SneakyThrows
  private double computeRoofArea(Geometry geometry) {
    Polygon polygon;
    if (geometry instanceof Polygon) {
      polygon = (Polygon) geometry;
    } else if (geometry instanceof MultiPolygon multiPolygon) {
      if (multiPolygon.getNumGeometries() != 1) {
        throw new IllegalArgumentException("RoofMultiPolygon must have exactly one geometry");
      }
      polygon = (Polygon) multiPolygon.getGeometryN(0);
    } else {
      throw new NotImplementedException(
          "Provided geometry instance not supported to compute roof area in square meter : "
              + geometry.getGeometryType());
    }

    var coords = polygon.getCoordinates();
    PolygonArea poly = new PolygonArea(Geodesic.WGS84, false);
    for (var point : coords) {
      poly.AddPoint(point.x, point.y);
    }
    return format(poly.Compute(false, false).area);
  }
}
