package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper.*;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionVGGRequested;
import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedObjectTypeWithPolygon;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.PolygonObjectTypeSerializable;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygonSerializable;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.CentroidGeometryRetriever;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExtendedImageWithDetectedObjectRequestedService
    implements Consumer<ExtendedImageWithDetectedObjectRequested> {
  private final TileFinder tileFinder;
  private final MachineDetectedTileRepository detectedTileRepository;
  private final BucketComponent bucketComponent;
  private final DetectedImageDraw detectedImageDraw;
  private final ExtenderApi extenderApi;
  private final FileWriter fileWriter;
  private final DetectionRepository detectionRepository;
  private final GeometryConverter geometryConverter;
  private final EventProducer eventProducer;
  private final CentroidGeometryRetriever centroidGeometryRetriever;

  @Override
  public void accept(ExtendedImageWithDetectedObjectRequested event) {
    var detectionId = event.getDetectionId();
    var detection = detectionRepository.findById(detectionId).orElseThrow();
    var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
    var providedFeatures = detection.getProvidedGeoJsonZone();

    var featureWithSurroundingTiles = getFeatureWithSurroundingTiles(providedFeatures);
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var featureWithDetectedObjects =
        getFeatureWithDetectedObjects(featureWithSurroundingTiles, machineDetectedTiles);
    var tiledPixelPolygons = getTiledPixelPolygons(featureWithDetectedObjects);
    var tiledPixelPolygonGroupedByFeature =
        tiledPixelPolygons.stream().collect(Collectors.groupingBy(TiledPixelPolygon::point));

    eventProducer.accept(
        List.of(
            new DetectionVGGRequested(
                detectionId, serializeTiledPixelPolygon(tiledPixelPolygonGroupedByFeature))));

    var featureWithObjectDrawnImages = computeDrawnImages(tiledPixelPolygonGroupedByFeature, layer);
    featureWithObjectDrawnImages.forEach(
        (feature, value) -> {
          var pointFeature = getPointOrCentroidAttribute(feature);
          var longitude = pointFeature.getCoordinates().getFirst();
          var latitude = pointFeature.getCoordinates().getLast();
          var extendedDrawnImageBase64 = extenderApi.apply(value);
          var filename = layer + "/extended_drawn_" + longitude + "_" + latitude;

          var extendedDrawnFile = fileWriter.base64ToFile(extendedDrawnImageBase64, filename);
          var bucketKey = filename + ".jpg";
          bucketComponent.upload(extendedDrawnFile, bucketKey);
        });
  }

  private List<TiledPixelPolygonSerializable> serializeTiledPixelPolygon(
      Map<Feature, List<TiledPixelPolygon>> collectedTiledPixelPolygonByFeature) {
    return collectedTiledPixelPolygonByFeature.entrySet().stream()
        .map(
            entry -> {
              var feature = entry.getKey();
              return entry.getValue().stream()
                  .map(
                      tiledPixelPolygon -> {
                        var polygonObjectTypeSerializable =
                            tiledPixelPolygon.polygons().stream()
                                .map(
                                    polygonObjectType ->
                                        new PolygonObjectTypeSerializable(
                                            geometryConverter.writeGeometryAsString(
                                                polygonObjectType.polygon()),
                                            polygonObjectType.objectType()))
                                .toList();
                        return new TiledPixelPolygonSerializable(
                            feature,
                            polygonObjectTypeSerializable,
                            tiledPixelPolygon.tileX(),
                            tiledPixelPolygon.tileY(),
                            tiledPixelPolygon.zoom());
                      })
                  .toList();
            })
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private List<TiledPixelPolygon> getTiledPixelPolygons(
      List<FeatureWithDetectedObjects> featureWithDetectedObjects) {
    return featureWithDetectedObjects.stream()
        .map(
            featureWithDetectedObject -> {
              var feature = featureWithDetectedObject.featureWithSurroundingTiles().feature();
              var detectedObjects = featureWithDetectedObject.detectedObjects();
              return detectedObjects.entrySet().stream()
                  .map(
                      entry -> {
                        var tileCoordinates = entry.getKey();
                        var x = tileCoordinates.getX();
                        var y = tileCoordinates.getY();
                        var z = tileCoordinates.getZ();
                        var pixelPolygonObjectType =
                            entry.getValue().stream()
                                .map(
                                    detectedObject -> {
                                      var polygon =
                                          geometryConverter.toPolygon(
                                              detectedObject
                                                  .getFeature()
                                                  .getGeometry()
                                                  .getMultiPolygon()
                                                  .getCoordinates());
                                      var detectableObjectType =
                                          detectedObject.getDetectableObjectType();
                                      return new PolygonObjectType(polygon, detectableObjectType);
                                    })
                                .toList();
                        return new TiledPixelPolygon(feature, pixelPolygonObjectType, x, y, z);
                      })
                  .toList();
            })
        .flatMap(List::stream)
        .toList();
  }

  private List<FeatureWithDetectedObjects> getFeatureWithDetectedObjects(
      List<FeatureWithSurroundingTiles> featureWithSurroundingTiles,
      List<MachineDetectedTile> detectedTileList) {
    return featureWithSurroundingTiles.stream()
        .map(
            featureWithTile -> {
              var listObject = new HashMap<TileCoordinates, List<DetectedObject>>();
              featureWithTile
                  .tileCoordinates()
                  .forEach(
                      tileCoordinate -> {
                        detectedTileList.forEach(
                            detectedTile -> {
                              var detectedTileCoordinate = detectedTile.getTile().getCoordinates();
                              if (tileCoordinate.equals(detectedTileCoordinate)) {
                                listObject.put(
                                    detectedTileCoordinate, detectedTile.getDetectedObjects());
                              }
                            });
                      });
              return new FeatureWithDetectedObjects(featureWithTile, listObject);
            })
        .toList();
  }

  private List<FeatureWithSurroundingTiles> getFeatureWithSurroundingTiles(
      List<Feature> providedFeatures) {
    return providedFeatures.stream()
        .map(
            feature -> {
              var geometry = feature.getGeometry().getActualInstance();
              var point = centroidGeometryRetriever.apply(geometry);
              var longitude = point.getCoordinates().getFirst();
              var latitude = point.getCoordinates().getLast();
              return new FeatureWithSurroundingTiles(
                  feature,
                  tileFinder.getSurroundingTiles(longitude, latitude, HOUSES_0.getZoomLevel()));
            })
        .toList();
  }

  private Map<Feature, List<File>> computeDrawnImages(
      Map<Feature, List<TiledPixelPolygon>> featureWithDetectedObjects, String layer) {
    Map<Feature, List<File>> drawnImageFiles = new HashMap<>();

    featureWithDetectedObjects.forEach(
        (feature, tiledPixelPolygons) -> {
          List<File> imageDrawn = new ArrayList<>();
          tiledPixelPolygons.sort(
              Comparator.comparing(TiledPixelPolygon::zoom)
                  .thenComparing(TiledPixelPolygon::tileY)
                  .thenComparing(TiledPixelPolygon::tileX));
          tiledPixelPolygons.forEach(
              tiledPixelPolygon -> {
                var originalImageKey =
                    layer
                        + "/"
                        + tiledPixelPolygon.zoom()
                        + "/"
                        + tiledPixelPolygon.tileX()
                        + "/"
                        + tiledPixelPolygon.tileY()
                        + ".jpg";
                var originalImage = bucketComponent.download(originalImageKey);

                var objectTypeWithPolygons =
                    tiledPixelPolygon.polygons().stream()
                        .map(
                            polygonObjectType -> {
                              List<List<BigDecimal>> polygonToPoints =
                                  geometryConverter.polygonToPoints(polygonObjectType.polygon());
                              var points =
                                  polygonToPoints.stream()
                                      .map(coor -> new Point().coordinates(coor))
                                      .toList();
                              return new DetectedObjectTypeWithPolygon(
                                  polygonObjectType.objectType(), points);
                            })
                        .toList();

                imageDrawn.add(detectedImageDraw.apply(originalImage, objectTypeWithPolygons));
              });
          drawnImageFiles.put(feature, imageDrawn);
        });

    return drawnImageFiles;
  }

  private record FeatureWithSurroundingTiles(
      Feature feature, List<TileCoordinates> tileCoordinates) {}

  private record FeatureWithDetectedObjects(
      FeatureWithSurroundingTiles featureWithSurroundingTiles,
      HashMap<TileCoordinates, List<DetectedObject>> detectedObjects) {}
}
