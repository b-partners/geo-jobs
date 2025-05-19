package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;

import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.endpoint.rest.model.Point;
import app.bpartners.geojobs.endpoint.rest.model.TileCoordinates;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedObjectTypeWithPolygon;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.detection.DetectedObject;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.service.DetectedImageDraw;
import app.bpartners.geojobs.service.tile19.ExtenderApi;
import app.bpartners.geojobs.service.tiling.TileFinder;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
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

  @Override
  public void accept(ExtendedImageWithDetectedObjectRequested event) {
    var detection = event.getDetection();
    var layer = detection.getGeoServerProperties().getGeoServerParameter().getLayers();

    if (!detection.hasOnlyPointsGeoJson()) {
      log.info(
          "Only detection with points geojson are supported for now, otherwise detection has"
              + " geoTypes {}",
          detection.getProvidedGeoJsonZone().stream().map(Feature::getType).toList());
      return;
    }
    var providedFeatures = detection.getProvidedGeoJsonZone();
    var pointWithSurroundingTiles = getPointWithSurroundingTiles(providedFeatures);
    var machineDetectedTiles = detectedTileRepository.findAllByZdjJobId(detection.getZdjId());
    var pointWithDetectedObjects =
        getPointWithDetectedObjects(pointWithSurroundingTiles, machineDetectedTiles);
    var pointWithObjectDrawnImages = computeDrawnImages(pointWithDetectedObjects, layer);

    pointWithObjectDrawnImages.forEach(
        point -> {
          var pointFeature = point.point().getGeometry().getPoint();
          var longitude = pointFeature.getCoordinates().getFirst();
          var latitude = pointFeature.getCoordinates().getLast();
          var extendedDrawnImage = extenderApi.apply(point.drawnImages());
          var filename = layer + "/extended_drawn_" + longitude + "_" + latitude + ".jpg";

          var extendedDrawnFile = fileWriter.base64ToFile(extendedDrawnImage, filename);
          bucketComponent.upload(extendedDrawnFile, filename);
        });
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

  private List<PointWithDrawnImage> computeDrawnImages(
      List<PointWithDetectedObjects> pointWithDetectedObjects, String layer) {
    return pointWithDetectedObjects.stream()
        .map(
            point -> {
              var surroundingCoordinates = point.pointWithSurroundingTiles().tileCoordinates();
              var drawnImage = new ArrayList<File>();
              surroundingCoordinates.forEach(
                  surroundingTileCoordinate -> {
                    var coordinatesWithObjects =
                        point.detectedObjects().entrySet().stream()
                            .filter(entry -> entry.getKey().equals(surroundingTileCoordinate))
                            .findAny()
                            .orElseThrow();
                    var coor = coordinatesWithObjects.getKey();
                    var detectedObjects = coordinatesWithObjects.getValue();

                    var originalImageKey =
                        layer + "/" + coor.getZ() + "/" + coor.getX() + "/" + coor.getY() + ".jpg";
                    var originalImage = bucketComponent.download(originalImageKey);
                    var objectTypeWithPolygons =
                        detectedObjects.stream()
                            .map(
                                detectedObject -> {
                                  var vggPoints =
                                      detectedObject
                                          .getFeature()
                                          .getGeometry()
                                          .getMultiPolygon()
                                          .getCoordinates()
                                          .getFirst()
                                          .getFirst()
                                          .stream()
                                          .map(
                                              vggCoordinate ->
                                                  new Point().coordinates(vggCoordinate))
                                          .toList();
                                  return new DetectedObjectTypeWithPolygon(
                                      detectedObject.getDetectedObjectType().getDetectableType(),
                                      vggPoints);
                                })
                            .toList();

                    drawnImage.add(detectedImageDraw.apply(originalImage, objectTypeWithPolygons));
                  });
              return new PointWithDrawnImage(point.pointWithSurroundingTiles().point(), drawnImage);
            })
        .toList();
  }

  private record PointWithDrawnImage(Feature point, List<File> drawnImages) {}

  private record PointWithSurroundingTiles(Feature point, List<TileCoordinates> tileCoordinates) {}

  private record PointWithDetectedObjects(
      PointWithSurroundingTiles pointWithSurroundingTiles,
      HashMap<TileCoordinates, List<DetectedObject>> detectedObjects) {}
}
