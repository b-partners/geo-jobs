package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static java.lang.Runtime.getRuntime;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toSet;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionProcessSucceeded;
import app.bpartners.geojobs.endpoint.event.model.RooferMadeDetectionCreated;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.geometry.IntXY;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.Parcel;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.MachineDetectedTile;
import app.bpartners.geojobs.repository.model.detection.ParcelDetectionTask;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RooferMadeDetectionCreatedService implements Consumer<RooferMadeDetectionCreated> {
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final TileObjectDetector detector;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeoJsonConverter geoJsonConverter;
  private final BucketComponent bucketComponent;
  private final DetectionMapper detectionMapper;
  private final FileWriter fileWriter;
  private final DetectionMaskCreator detectionMaskCreator;
  private final EventProducer eventProducer;
  private final ExecutorService executorService =
      newFixedThreadPool(Math.max(1, getRuntime().availableProcessors() - 1));

  @Override
  public void accept(RooferMadeDetectionCreated event) {
    var detection = detectionRepository.findById(event.getDetectionId()).orElseThrow();
    var tileMasks = detectionMaskCreator.apply(detection.getProvidedGeoJsonZone());
    var detectionConf = detection.getDetectableObjectConfigurations();

    var zdj = zoneDetectionJobService.getMachineZdjFromZdjId(event.getZdjId());
    var parcels =
        zoneDetectionJobService.getTasks(zdj).stream()
            .map(ParcelDetectionTask::getParcel)
            .collect(toSet());
    var detectionTasks = mapToDetectionTask(parcels);
    try {
      List<MachineDetectedTile> machineDetectedTiles =
          executorService
              .invokeAll(
                  detectionTasks.stream()
                      .map(
                          task ->
                              ((Callable<MachineDetectedTile>)
                                  () -> {
                                    var coords = task.getTile().getCoordinates();
                                    var tile = new IntXY(coords.getX(), coords.getY());
                                    var mask = tileMasks.getOrDefault(tile, createTempImage());
                                    var response = detector.apply(task, mask, detectionConf);
                                    return detectionMapper.toDetectedTile(
                                        response,
                                        task.getTile(),
                                        task.getParcelId(),
                                        zdj.getId(),
                                        null);
                                  }))
                      .collect(toSet()))
              .stream()
              .map(this::futureStream)
              .toList();
      var saved = machineDetectedTileRepository.saveAll(machineDetectedTiles);

      var detectedTiles =
          saved.stream()
              .map(
                  machineDetectedTile ->
                      DetectedTile.builder()
                          .tile(machineDetectedTile.getTile())
                          .detectedObjects(machineDetectedTile.getDetectedObjects())
                          .build())
              .toList();

      processGeoJsonConversion(detection, zdj, detectedTiles);
    } catch (InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }

  private void processGeoJsonConversion(
      Detection detection, ZoneDetectionJob zdj, List<DetectedTile> detectedTiles) {
    var zdjId = zdj.getId();
    var geoJson = geoJsonConverter.convert(detectedTiles);
    var zoneName = zdj.getZoneName();
    var fileKey = GEO_JSON_BUCKET_FOLDER + zdjId + "/" + zoneName + GEO_JSON_EXTENSION;
    var geoJsonAsByte = geoJson.getStringValue().getBytes();
    var geoJsonAsFile =
        fileWriter.write(geoJsonAsByte, createTempDirectory(), zoneName + GEO_JSON_EXTENSION);
    bucketComponent.upload(geoJsonAsFile, fileKey);
    detection.setGeojsonS3FileKey(fileKey);
    detectionRepository.save(detection);

    eventProducer.accept(
        List.of(GeoJsonConversionProcessSucceeded.builder().detection(detection).build()));
  }

  private MachineDetectedTile futureStream(Future<MachineDetectedTile> future) {
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Set<TileDetectionTask> mapToDetectionTask(Set<Parcel> parcels) {
    return parcels.stream()
        .map(
            parcel -> {
              var parcelId = parcel.getId();
              var tiles = parcel.getParcelContent().getTiles();
              return tiles.stream()
                  .map(
                      tile ->
                          TileDetectionTask.builder()
                              .id(randomUUID().toString())
                              .parcelId(parcelId)
                              .tile(tile)
                              .build());
            })
        .flatMap(Stream::distinct)
        .collect(toSet());
  }

  private File createTempImage() {
    BufferedImage blackImage = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = blackImage.createGraphics();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, 1024, 1024);
    g.dispose();

    try {
      File file = File.createTempFile("black_image_", ".png");
      ImageIO.write(blackImage, "png", file);
      return file;
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e.getMessage());
    }
  }
}
