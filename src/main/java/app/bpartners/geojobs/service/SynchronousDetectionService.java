package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.DetectionStepName.MACHINE_DETECTION;
import static app.bpartners.geojobs.job.model.Status.HealthStatus.SUCCEEDED;
import static app.bpartners.geojobs.job.model.Status.ProgressionStatus.FINISHED;

import app.bpartners.geojobs.concurrency.Workers;
import app.bpartners.geojobs.endpoint.event.model.ExtendedImageWithDetectedObjectRequested;
import app.bpartners.geojobs.endpoint.rest.mapper.DetectionFromStatisticRestMapper;
import app.bpartners.geojobs.endpoint.rest.model.*;
import app.bpartners.geojobs.repository.DetectionRepository;
import app.bpartners.geojobs.service.detection.*;
import app.bpartners.geojobs.service.event.ExtendedImageWithDetectedObjectRequestedService;
import app.bpartners.geojobs.service.geojson.GeoJsonConversionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
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
  private final ExtendedImageWithDetectedObjectRequestedService
      extendedImageWithDetectedObjectRequestedService;
  private final GeoJsonConversionJobService geoJsonConversionJobService;
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final Workers workers;

  @Override
  public Detection apply(app.bpartners.geojobs.repository.model.detection.Detection detection) {
    // Tiling step
    var detectionWithCreatedZTJ = detectionTilingCreation.processTiling(detection);
    var zoneTilingJobId = detectionWithCreatedZTJ.getZtjId();
    var tilingTasks = zoneTilingJobService.consumeTasks(zoneTilingJobId);
    var finishedZoneTilingJob = zoneTilingJobService.findById(zoneTilingJobId);

    List<Callable<Void>> voidCallable1 =
        List.of(
            () -> {
              // Original image retriever step
              detectionDelimitationRetriever.accept(detectionWithCreatedZTJ, true);
              return null;
            },
            () -> {
              // Machine detection step
              detectionMachineDetectionCreation.processMachineDetection(
                  detectionWithCreatedZTJ, finishedZoneTilingJob, tilingTasks);
              return null;
            });
    workers.invokeAll(voidCallable1);

    var detectionWithZDJFinished = detectionRepository.findById(detection.getId()).orElseThrow();
    List<Callable<Void>> voidCallable2 =
        List.of(
            () -> {
              // VGG result computing with drawn image step
              extendedImageWithDetectedObjectRequestedService.accept(
                  new ExtendedImageWithDetectedObjectRequested(
                      detectionWithZDJFinished.getId(), true));
              return null;
            },
            () -> {
              // GeoJson Result Requested __EVENT__ step
              var finishedZoneDetectionJob =
                  zoneDetectionJobService.findById(detectionWithZDJFinished.getZdjId());
              geoJsonConversionJobService.getOrComputeGeoJsonConversionJob(
                  finishedZoneDetectionJob);
              return null;
            });
    workers.invokeAll(voidCallable2);

    return detectionFromStatisticRestMapper.computeEmptyStatisticFromStep(
        detectionRepository.findById(detectionWithZDJFinished.getId()).orElseThrow(),
        FINISHED,
        SUCCEEDED,
        MACHINE_DETECTION);
  }
}
