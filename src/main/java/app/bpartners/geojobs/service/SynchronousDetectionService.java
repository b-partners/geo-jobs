package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;
import static java.util.UUID.randomUUID;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.DetectionRoofSlopeAndHeightRequested;
import app.bpartners.geojobs.endpoint.event.model.ZoneImageRequested;
import app.bpartners.geojobs.endpoint.event.model.ZoneVggRequested;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.DetectableObjectConfigurationRepository;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.event.ZoneImageRequestedService;
import app.bpartners.geojobs.service.event.ZoneVggRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SynchronousDetectionService
    implements Function<app.bpartners.geojobs.repository.model.detection.Detection, Detection> {
  private final DetectionRepository detectionRepository;
  private final DetectionFromStatisticRestMapper detectionFromStatisticRestMapper;
  private final DetectionTilingCreation detectionTilingCreation;
  private final ZoneTilingJobService zoneTilingJobService;
  private final DetectionMachineDetectionCreation detectionMachineDetectionCreation;
  private final DetectionDelimitationRetriever detectionDelimitationRetriever;
  private final ZoneVggRequestedService zoneVggRequestedService;
  private final GeoJsonConversionJobService geoJsonConversionJobService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final Workers workers;
  private final DetectableObjectConfigurationRepository detectableObjectConfigurationRepository;
  private final ZoneImageRequestedService zoneImageRequestedService;
  private final EventProducer eventProducer;

  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    // Tiling step
    var detectionWithCreatedZTJ = detectionTilingCreation.processTiling(detection);
    var zoneTilingJobId = detectionWithCreatedZTJ.getZtjId();
    var tilingTasks = zoneTilingJobService.consumeTasks(zoneTilingJobId);
    var finishedZoneTilingJob = zoneTilingJobService.findById(zoneTilingJobId);

    // Machine detection job creation
    var createdZoneDetectionJob = zoneDetectionJobService.saveZDJFromZTJ(finishedZoneTilingJob);
    var zoneDetectionJobDetectableConf =
        detection.getDetectableObjectConfigurations().stream()
            .map(
                detectableObjectConfiguration ->
                    detectableObjectConfiguration.duplicate(
                        randomUUID().toString(), createdZoneDetectionJob.getId()))
            .toList();
    detectableObjectConfigurationRepository.saveAll(zoneDetectionJobDetectableConf);

    var detectionWithCreatedZDJ =
        detectionRepository.save(
            detectionWithCreatedZTJ.toBuilder().zdjId(createdZoneDetectionJob.getId()).build());

    detectionDelimitationRetriever.accept(detectionWithCreatedZDJ);

    // Has to be async for the moment
    eventProducer.accept(
        List.of(
            DetectionRoofSlopeAndHeightRequested.builder().detectionId(detection.getId()).build()));

    // TODO: is PointExtendedImageRequest still necessary ?
    List<Callable<Void>> imageRequestCallableVoidList =
        detectionWithCreatedZDJ.getProvidedGeoJsonZone().stream()
            .map(
                providedFeature ->
                    (Callable<Void>)
                        () -> {
                          zoneImageRequestedService.accept(
                              new ZoneImageRequested(detection.getId()));
                          return null;
                        })
            .toList();
    Callable<Void> machineDetectionProcessCallableVoidList =
        () -> {
          // Machine detection step
          detectionMachineDetectionCreation.processMachineDetection(
              detectionWithCreatedZDJ, createdZoneDetectionJob, tilingTasks);
          return null;
        };
    List<Callable<Void>> firstCallableVoidList = new ArrayList<>(imageRequestCallableVoidList);
    firstCallableVoidList.add(machineDetectionProcessCallableVoidList);
    workers.invokeAll(firstCallableVoidList);

    List<Callable<Void>> secondVoidCallable =
        List.of(
            () -> {
              // VGG result computing step
              zoneVggRequestedService.accept(new ZoneVggRequested(detection.getId()));
              return null;
            },
            () -> {
              // GeoJson Result Requested __EVENT__ step
              geoJsonConversionJobService.getOrComputeGeoJsonConversionJob(createdZoneDetectionJob);
              return null;
            });
    workers.invokeAll(secondVoidCallable);

    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detectionRepository.findById(detection.getId()).orElseThrow(),
        FINISHED,
        SUCCEEDED,
        MACHINE_DETECTION);
  }
}
