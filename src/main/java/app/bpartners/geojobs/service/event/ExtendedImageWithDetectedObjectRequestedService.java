package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedObjectTypeWithPolygon;
import app.bpartners.geojobs.model.geometry.PolygonObjectType;
import app.bpartners.geojobs.model.geometry.TiledPixelPolygon;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import app.bpartners.geojobs.service.tiling.TiledPixelPolygonFilter;
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
  private final TiledPixelPolygonFilter tiledPixelPolygonFilter;
  private final GeometryConverter geometryConverter;

  @Override
  public void accept(ExtendedImageWithDetectedObjectRequested event) {
    var detectionId = event.getDetectionId();
    var detection = detectionRepository.findById(detectionId).orElseThrow();
    var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();
    var providedFeatures = detection.getProvidedGeoJsonZone();

    log.info("Detection to be compute image with detected obj : {}", detection);

    if (!detection.hasOnlyPointsGeoJson()) {
      log.info(
          "Only detection with points geojson are supported for now, otherwise detection has"
              + " geoTypes {}",
          providedFeatures.stream()
              .map(
                  feature ->
                      Objects.requireNonNull(feature.getGeometry())
                          .getActualInstance()
                          .getClass()
                          .getSimpleName())
              .toList());
      return;
    }
    var pointDelimitation = detection.getPointDelimitation();
    if (pointDelimitation == null || pointDelimitation.isEmpty()) {
      log.info("Only detection with point delimitations are supported for now");
      return;
    }

    var pointWithSurroundingTiles = getPointWithSurroundingTiles(providedFeatures);
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var pointWithDetectedObjects =
        getPointWithDetectedObjects(pointWithSurroundingTiles, machineDetectedTiles);
    var tiledPixelPolygons = getTiledPixelPolygonsGroupByPoint(pointWithDetectedObjects);
    var masks =
        pointDelimitation.entrySet().stream()
            .map(
                entry ->
                    geometryConverter.apply(
                        entry.getValue().getGeometry().getMultiPolygon().getCoordinates()))
            .toList();
    var filteredTiledPixelPolygonByMask =
        masks.stream()
            .map(mask -> tiledPixelPolygonFilter.filterPolygonsInMask(tiledPixelPolygons, mask))
            .flatMap(List::stream)
            .collect(Collectors.groupingBy(TiledPixelPolygon::point));
    var pointWithObjectDrawnImages = computeDrawnImages(filteredTiledPixelPolygonByMask, layer);

    pointWithObjectDrawnImages
        .entrySet()
        .forEach(
            entry -> {
              var point = entry.getKey();
              var pointFeature = point.getGeometry().getPoint();
              var longitude = pointFeature.getCoordinates().getFirst();
              var latitude = pointFeature.getCoordinates().getLast();
              var extendedDrawnImageBase64 = extenderApi.apply(entry.getValue());
              var filename = layer + "/extended_drawn_" + longitude + "_" + latitude;

              var extendedDrawnFile = fileWriter.base64ToFile(extendedDrawnImageBase64, filename);
              var bucketKey = filename + ".jpg";
              bucketComponent.upload(extendedDrawnFile, bucketKey);
            });
  }

  private List<TiledPixelPolygon> getTiledPixelPolygonsGroupByPoint(
      List<PointWithDetectedObjects> pointWithDetectedObjects) {
    return pointWithDetectedObjects.stream()
        .map(
            pointWithDetectedObject -> {
              var point = pointWithDetectedObject.pointWithSurroundingTiles().point();
              var detectedObjects = pointWithDetectedObject.detectedObjects();
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
                        return new TiledPixelPolygon(point, pixelPolygonObjectType, x, y, z);
                      })
                  .toList();
            })
        .flatMap(List::stream)
        .toList();
  }

  private List<PointWithDetectedObjects> getPointWithDetectedObjects(
      List<PointWithSurroundingTiles> pointWithSurroundingTiles,
      List<MachineDetectedTile> detectedTileList) {
    return pointWithSurroundingTiles.stream()
        .map(
            pointWithTiles -> {
              var listObject = new HashMap<TileCoordinates, List<DetectedObject>>();
              pointWithTiles
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
              return new PointWithDetectedObjects(pointWithTiles, listObject);
            })
        .toList();
  }

  private List<PointWithSurroundingTiles> getPointWithSurroundingTiles(
      List<Feature> providedFeatures) {
    return providedFeatures.stream()
        .map(
            feature -> {
              var point = feature.getGeometry().getPoint();
              var longitude = point.getCoordinates().getFirst();
              var latitude = point.getCoordinates().getLast();
              return new PointWithSurroundingTiles(
                  feature,
                  tileFinder.getSurroundingTiles(longitude, latitude, HOUSES_0.getZoomLevel()));
            })
        .toList();
  }

  private Map<Feature, List<File>> computeDrawnImages(
      Map<Feature, List<TiledPixelPolygon>> pointWithDetectedObjects, String layer) {
    Map<Feature, List<File>> drawnImageFiles = new HashMap<>();

    pointWithDetectedObjects.forEach(
        (featurePoint, tiledPixelPolygons) -> {
          List<File> imageDrawn = new ArrayList<>();
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
          drawnImageFiles.put(featurePoint, imageDrawn);
        });

    return drawnImageFiles;
  }

  private record PointWithSurroundingTiles(Feature point, List<TileCoordinates> tileCoordinates) {}

  private record PointWithDetectedObjects(
      PointWithSurroundingTiles pointWithSurroundingTiles,
      HashMap<TileCoordinates, List<DetectedObject>> detectedObjects) {}
}
