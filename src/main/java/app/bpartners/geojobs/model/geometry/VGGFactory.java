package app.bpartners.geojobs.model.geometry;

import static app.bpartners.geojobs.model.geometry.GeometryFactory.geometryFactory;
import static app.bpartners.geojobs.model.geometry.area.AreaRateComputerFacade.*;
import static app.bpartners.geojobs.repository.model.detection.DetectableType.TOITURE_REVETEMENT;
import static app.bpartners.geojobs.service.geojson.GeometryConverter.unifyMultiPolygon;
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
      var confidence = metadata.get("confidence");
      var confidenceAsDouble =
          confidence == null ? null : Double.parseDouble(confidence.toString());
      Map<String, VGG.Annotation.Region> newRegions = new HashMap<>();
      newRegions.put(
          String.valueOf(Instant.now().getNano()), toVGGRegion(label, confidenceAsDouble, null, p));
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

  public VGG from(Set<TiledPolygon> polygons) {
    var polygonsWithMetadata =
        polygons.stream()
            .map(
                p -> {
                  var metadata = new HashMap<>();
                  metadata.put("filename", filename(p.originTile()));
                  metadata.put("label", p.type().name());
                  var polygon = p.polygon();
                  polygon.setUserData(metadata);
                  return polygon;
                })
            .collect(Collectors.toSet());
    return convert(polygonsWithMetadata);
  }

  private static String filename(IntXY originTile) {
    return String.format("%s_%s_%s.jpg", 20, originTile.x(), originTile.y());
  }

  public Map<Feature, VGG> from(
      List<TiledPixelPolygon> tiledPixelPolygons, MultiPolygon roofLatLonMultiPolygon) {
    Map<Feature, List<TiledPixelPolygon>> tiledPixelPolygonFilteredByPoint =
        tiledPixelPolygons.stream().collect(Collectors.groupingBy(TiledPixelPolygon::point));
    var vggMap = new HashMap<Feature, VGG>();
    int minTileXGlobal =
        tiledPixelPolygons.stream().mapToInt(TiledPixelPolygon::tileX).min().orElseThrow();
    int minTileYGlobal =
        tiledPixelPolygons.stream().mapToInt(TiledPixelPolygon::tileY).min().orElseThrow();
    int zoom =
        tiledPixelPolygons.stream().mapToInt(TiledPixelPolygon::zoom).findAny().orElseThrow();

    // TODO: convert provided into pixel
    var roofPixelPolygon =
        tiledPixelPolygons.stream()
            .map(
                tiledPolygon -> {
                  var toiturePolygonList =
                      tiledPolygon.polygons().stream()
                          .filter(
                              polygonObjectType ->
                                  TOITURE_REVETEMENT.equals(polygonObjectType.objectType()))
                          .toList();
                  return toiturePolygonList.stream()
                      .map(
                          polygonObjectType -> {
                            var projectedPolygonsToCompositeImage =
                                projectPolygonsToCompositeImage(
                                    tiledPolygon.tileX(),
                                    tiledPolygon.tileY(),
                                    minTileXGlobal,
                                    minTileYGlobal,
                                    DEFAULT_IMG_SIZE,
                                    polygonObjectType.polygon());
                            return geometryFactory.createMultiPolygon(
                                new Polygon[] {projectedPolygonsToCompositeImage});
                          })
                      .reduce(unifyMultiPolygon())
                      .orElseThrow();
                })
            .reduce(unifyMultiPolygon())
            .orElseThrow();

    tiledPixelPolygonFilteredByPoint.forEach(
        (featurePoint, tiledPolygons) -> {
          var vgg = new VGG();
          int minTileXForPoint =
              tiledPolygons.stream().mapToInt(TiledPixelPolygon::tileX).min().orElseThrow();
          int minTileYForPoint =
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
                                      minTileXForPoint,
                                      minTileYForPoint,
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
                              (polygonObjectType.polygon().getArea() / roofPixelPolygon.getArea())
                                  * 100);
                      regions.put(
                          String.valueOf(System.nanoTime()),
                          toVGGRegion(label.name(), null, rate, detectedObjectPolygon));
                    });
                var originTileCoords = new IntXY(minTileXGlobal, minTileYGlobal);
                var tilingConf = new TilingConf(zoom, DEFAULT_IMG_SIZE);
                var convertedLatLonRoofPolygon =
                    new TiledPolygon(
                            (Polygon) roofLatLonMultiPolygon.getGeometryN(0),
                            null,
                            originTileCoords,
                            tilingConf)
                        .latLonPolygon()
                        .polygon();
                var properties =
                    computeProperties(
                        convertedLatLonRoofPolygon, roofPixelPolygon, originalPolygonObjectTypes);
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
      Geometry lonLatRoofPolygon,
      Geometry pixelRoofPolygon,
      List<PolygonObjectType> originalPolygonObjectTypes) {
    var rateComputer = new AreaRateComputerFacade(pixelRoofPolygon, originalPolygonObjectTypes);
    var usureRate = rateComputer.getUsureAreaRate();
    var humiditeRate = rateComputer.getHumidityAreaRate();
    var moisissureRate = rateComputer.getMoisissureAreaRate();
    var globalRateValue = rateComputer.getGlobalRate();
    var globalRateType = rateComputer.getRate();

    var properties = new HashMap<String, Object>();

    properties.put("roof_area_in_m2", computeRoofArea(lonLatRoofPolygon));
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
