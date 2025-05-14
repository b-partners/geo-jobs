package app.bpartners.geojobs.service.event;

import static app.bpartners.geojobs.file.FileWriter.createTempDirectory;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_BUCKET_FOLDER;
import static app.bpartners.geojobs.service.event.GeoJsonConversionTaskConsumer.GEO_JSON_EXTENSION;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblyInitiated;
import app.bpartners.geojobs.endpoint.event.model.GeoJsonConversionAssemblySucceeded;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.FeatureMapper;
import app.bpartners.geojobs.endpoint.rest.model.Feature;
import app.bpartners.geojobs.file.FileWriter;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.model.exception.NotFoundException;
import app.bpartners.geojobs.model.geometry.polygon.PolygonAddress;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionJobRepository;
import app.bpartners.geojobs.repository.GeoJsonConversionTaskRepository;
import app.bpartners.geojobs.repository.model.detection.Detection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.geojson.GeoJsonConversionTask;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.geojson.GeoJson;
import app.bpartners.geojobs.service.geojson.GeoJsonMapper;
import app.bpartners.geojobs.service.geojson.GeometryConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoJsonConversionAssemblyInitiatedService
    implements Consumer<GeoJsonConversionAssemblyInitiated> {
  private static final double HALF_OF_AREA = 0.5;
  private final GeoJsonConversionTaskRepository geoJsonConversionTaskRepository;
  private final GeoJsonConversionJobRepository geoJsonConversionJobRepository;
  private final BucketComponent bucketComponent;
  private final FileWriter fileWriter;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final DetectionRepository detectionRepository;
  private final EventProducer eventProducer;
  private final ObjectMapper objectMapper;
  private final FeatureMapper featureMapper;
  private final GeometryConverter geometryConverter;
  private final GeoJsonMapper geoJsonMapper;

  @Override
  public void accept(GeoJsonConversionAssemblyInitiated event) {
    var conversionJobId = event.getGeoJsonConversionJobId();
    var conversionTasks = geoJsonConversionTaskRepository.findAllByJobId(conversionJobId);
    var geoJsonConversionJob =
        geoJsonConversionJobRepository.findById(conversionJobId).orElseThrow();
    var zoneDetectionJob =
        zoneDetectionJobService.findById(geoJsonConversionJob.getZoneDetectionJobId());
    var detection = getDetection(zoneDetectionJob);

    var polygonAddressDelimitation = getPolygonAddressDelimitation(detection);
    var geoFeaturesList = getGeoFeaturesList(conversionTasks);
    var geoFeaturesFilteredByAddresses =
        filterGeoFeaturesByAddresses(geoFeaturesList, polygonAddressDelimitation);

    var geoJson =
        new GeoJson(
            geoFeaturesFilteredByAddresses.isEmpty()
                ? geoFeaturesList
                : geoFeaturesFilteredByAddresses);
    var outputFileName = zoneDetectionJob.getZoneName() + "-final" + GEO_JSON_EXTENSION;
    var geoJsonFinalFile =
        fileWriter.write(
            geoJson.getStringValue().getBytes(StandardCharsets.UTF_8),
            createTempDirectory(),
            outputFileName);

    var combinedFileKey = GEO_JSON_BUCKET_FOLDER + zoneDetectionJob.getId() + "/" + outputFileName;

    bucketComponent.upload(geoJsonFinalFile, combinedFileKey);

    var savedConversionJob =
        geoJsonConversionJobRepository.save(
            geoJsonConversionJob.toBuilder().fileKey(combinedFileKey).build());
    if (zoneDetectionJob.isFinished()) {
      if (detection != null) {
        detectionRepository.save(
            detection.toBuilder().geojsonS3FileKey(savedConversionJob.getFileKey()).build());
      }
      eventProducer.accept(
          List.of(
              GeoJsonConversionAssemblySucceeded.builder()
                  .geoJsonConversionJob(savedConversionJob)
                  .build()));
    }
  }

  private List<GeoJson.GeoFeature> filterGeoFeaturesByAddresses(
      List<GeoJson.GeoFeature> geoFeaturesList, List<PolygonAddress> polygonAddressDelimitation) {
    var geoFeaturesGroupByAddress =
        geoFeaturesList.stream()
            .filter(
                geoFeature -> {
                  var optionalPolygonAddress =
                      polygonAddressDelimitation.stream()
                          .filter(
                              polygonAddress ->
                                  polygonAddress
                                      .address()
                                      .equals(geoFeature.getProperties().get("address")))
                          .findAny();
                  if (optionalPolygonAddress.isEmpty()) {
                    return false;
                  }
                  var roofPolygon = optionalPolygonAddress.get().polygon();
                  var objectPolygon =
                      featureMapper.toDomainPolygon(
                          Objects.requireNonNull(geoFeature.getGeometry().getCoordinates()));
                  var intersection = roofPolygon.intersection(objectPolygon);
                  double intersectionArea = intersection.getArea();
                  double objectPolygonArea = objectPolygon.getArea();
                  double ratio = intersectionArea / objectPolygonArea;
                  return ratio > HALF_OF_AREA;
                })
            .collect(
                Collectors.groupingBy(
                    geoFeature -> geoFeature.getProperties().get("address").toString()));

    return geoFeaturesGroupByAddress.entrySet().stream()
        .map(this::unifyFeatureGeometryByAddress)
        .toList();
  }

  private GeoJson.GeoFeature unifyFeatureGeometryByAddress(
      Map.Entry<String, List<GeoJson.GeoFeature>> entry) {
    var multiPolygonsLinkedByAddress =
        entry.getValue().stream()
            .map(
                geoFeature ->
                    geometryConverter.apply(
                        Objects.requireNonNull(geoFeature.getGeometry().getCoordinates())))
            .toList();
    var unifiedMultiPolygon = geometryConverter.unifyMultiPolygon(multiPolygonsLinkedByAddress);
    var address = entry.getKey();
    var properties = new HashMap<String, Object>();
    properties.put("address", address);
    return geoJsonMapper.getGeoFeature(
        geometryConverter.multiPolygonToNestedList(unifiedMultiPolygon), properties);
  }

  private List<PolygonAddress> getPolygonAddressDelimitation(Detection detection) {
    if (detection == null
        || detection.getMultiPolygonGeoJsonZone() == null
        || detection.getMultiPolygonGeoJsonZone().isEmpty()) {
      return Collections.emptyList();
    }
    Map<String, List<Feature>> featureWithAddresses =
        detection.getMultiPolygonGeoJsonZone().stream()
            .filter(
                feature ->
                    feature.getProperties() != null
                        && feature.getProperties().get("address") != null)
            .collect(
                Collectors.groupingBy(
                    feature -> feature.getProperties().get("address").toString()));

    return featureWithAddresses.entrySet().stream()
        .map(
            stringListEntry -> {
              var address = stringListEntry.getKey();
              var polygon =
                  stringListEntry.getValue().stream()
                      .map(featureMapper::toDomain)
                      .reduce(((acc, p) -> (Polygon) p.union(acc)))
                      .orElseThrow(
                          () ->
                              new NotFoundException(
                                  "No polygon delimitation found for address :" + address));
              return new PolygonAddress(address, polygon);
            })
        .toList();
  }

  private Detection getDetection(ZoneDetectionJob zoneDetectionJob) {
    ZoneDetectionJob machineZDJ =
        zoneDetectionJobService.getMachineZdjFromZdjId(zoneDetectionJob.getId());
    ZoneDetectionJob humanZDJ;
    try {
      humanZDJ = zoneDetectionJobService.getHumanZdjFromZdjId(zoneDetectionJob.getId());
    } catch (IllegalArgumentException ignored) {
      humanZDJ = null;
    }
    return detectionRepository
        .findByZdjId(humanZDJ == null ? null : humanZDJ.getId())
        .orElseGet(
            () -> {
              var optionalDetectionFromMachineZDJ =
                  detectionRepository.findByZdjId(machineZDJ.getId());
              if (optionalDetectionFromMachineZDJ.isPresent()) {
                return optionalDetectionFromMachineZDJ.orElseThrow();
              }
              return null;
            });
  }

  private List<GeoJson.GeoFeature> getGeoFeaturesList(List<GeoJsonConversionTask> conversionTasks) {
    var partialConvertedGeoJsonFiles =
        conversionTasks.stream()
            .map(conversionTask -> bucketComponent.download(conversionTask.getFileKey()))
            .toList();
    return partialConvertedGeoJsonFiles.stream()
        .map(
            file -> {
              try {
                List<GeoJson.GeoFeature> geoFeatures =
                    objectMapper.readValue(file, new TypeReference<>() {});
                return geoFeatures;
              } catch (IOException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            })
        .flatMap(List::stream)
        .toList();
  }
}
