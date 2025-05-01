package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static app.bpartners.geojobs.repository.model.ArcgisImageZoom.HOUSES_0;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionProcessSucceeded;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.endpoint.rest.security.AuthProvider;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.mail.Email;
import app.bpartners.geojobs.mail.Mailer;
import app.bpartners.geojobs.model.DetectedTile;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.MachineDetectedTileRepository;
import app.bpartners.geojobs.repository.model.TileDetectionTask;
import app.bpartners.geojobs.repository.model.tiling.Tile;
import app.bpartners.geojobs.service.detection.DetectionMapper;
import app.bpartners.geojobs.service.detection.DetectionMaskCreator;
import app.bpartners.geojobs.service.detection.TileObjectDetector;
import app.bpartners.geojobs.service.geojson.GeoJsonConverter;
import app.bpartners.geojobs.template.HTMLTemplateParser;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@AllArgsConstructor
public class RooferDetectionService
    implements Function<app.bpartners.geojobs.repository.model.detection.Detection, Detection> {
  private static final String TEMPLATE_NAME = "roofer_detection_made";
  private static final String env = System.getenv("ENV");
  private final TileObjectDetector detector;
  private final DetectionMaskCreator detectionMaskCreator;
  private final DetectionMapper detectionMapper;
  private final MachineDetectedTileRepository machineDetectedTileRepository;
  private final GeoJsonConverter geoJsonConverter;
  private final FileWriter fileWriter;
  private final DetectionRepository detectionRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<GeoJsonConversionProcessSucceeded> eventProducer;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final Mailer mailer;
  private final AuthProvider authProvider;
  private final HTMLTemplateParser htmlTemplateParser;

  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    var providedGeoJson = detection.getProvidedGeoJsonZone();
    int zoom =
        providedGeoJson.getFirst().getProperties().get("zoom") == null
            ? HOUSES_0.getZoomLevel()
            : (Integer) providedGeoJson.getFirst().getProperties().get("zoom");
    var flattedFeatures =
        providedGeoJson.stream()
            .map(app.bpartners.geojobs.endpoint.rest.model.Feature::getGeometry)
            .filter(Objects::nonNull)
            .map(FeatureGeometry::getMultiPolygon)
            .map(MultiPolygon::getCoordinates)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .flatMap(List::stream)
            .toList();
    var mask = detectionMaskCreator.apply(flattedFeatures);

    var tile =
        Tile.builder()
            .coordinates(new TileCoordinates().x(0).y(0).z(zoom))
            .bucketPath(detection.getImageFileKey())
            .build();
    var toDetect =
        TileDetectionTask.builder()
            .id(randomUUID().toString())
            .jobId(detection.getId())
            .tile(tile)
            .build();
    var detectionResponse =
        detector.apply(toDetect, mask, detection.getDetectableObjectConfigurations());
    var machineDetectedTile =
        detectionMapper.toDetectedTile(detectionResponse, tile, null, null, null);
    machineDetectedTileRepository.save(machineDetectedTile);
    var detectedTile =
        DetectedTile.builder()
            .tile(machineDetectedTile.getTile())
            .detectedObjects(machineDetectedTile.getDetectedObjects())
            .build();

    processGeoJsonConversion(detection, List.of(detectedTile), detectionResponse.getRstImageUrl());
    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detection, FINISHED, SUCCEEDED, MACHINE_DETECTION);
  }

  private void processGeoJsonConversion(
      app.bpartners.geojobs.repository.model.detection.Detection detection,
      List<DetectedTile> detectedTiles,
      String detectionResultUrl) {
    var zdjId = detection.getId();
    var geoJson = geoJsonConverter.convert(detectedTiles);
    var zoneName = detection.getZoneName();
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

  @SneakyThrows
  public void sendEmail(Prospect prospect, File detectionResultPdf) {
    var formattedCreationDatetime =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .format(now().atZone(ZoneId.of("Europe/Paris")));
    Context context = new Context();
    context.setVariable("firstName", prospect.getFirstName());
    context.setVariable("lastName", prospect.getLastName());
    context.setVariable("phoneNumber", prospect.getPhone());
    context.setVariable("email", prospect.getEmail());
    context.setVariable("address", prospect.getAddress());
    context.setVariable("creationDatetime", formattedCreationDatetime);
    var emailBody = htmlTemplateParser.apply(TEMPLATE_NAME, context);

    mailer.accept(
        new Email(
            new InternetAddress(authProvider.getAuthenticatedCommunity().getEmail()),
            List.of(new InternetAddress("tech@bpartners.app")),
            List.of(),
            String.format("[%s] - ANALYSE TOITURE", env),
            emailBody,
            List.of(detectionResultPdf)));
  }
}
