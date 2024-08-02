package app.bpartners.geojobs.service;

import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.MACHINE_DETECTION;
import static app.bpartners.geojobs.endpoint.rest.model.JobTypes.TILING;
import static app.bpartners.geojobs.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob.DetectionType.MACHINE;
import static app.bpartners.geojobs.service.tiling.ZoneTilingJobService.getTilingTasks;

import app.bpartners.geojobs.endpoint.event.EventProducer;
import app.bpartners.geojobs.endpoint.event.model.ZDJStatusRecomputingSubmitted;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.TaskStatisticMapper;
import app.bpartners.geojobs.endpoint.rest.controller.mapper.ZoneTilingJobMapper;
import app.bpartners.geojobs.endpoint.rest.model.CreateFullDetection;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectConfiguration;
import app.bpartners.geojobs.endpoint.rest.model.DetectableObjectType;
import app.bpartners.geojobs.endpoint.rest.model.FullDetectedZone;
import app.bpartners.geojobs.endpoint.rest.model.JobTypes;
import app.bpartners.geojobs.endpoint.rest.validator.ZoneDetectionJobValidator;
import app.bpartners.geojobs.file.BucketComponent;
import app.bpartners.geojobs.job.model.statistic.TaskStatistic;
import app.bpartners.geojobs.model.exception.ApiException;
import app.bpartners.geojobs.repository.FullDetectionRepository;
import app.bpartners.geojobs.repository.ZoneDetectionJobRepository;
import app.bpartners.geojobs.repository.ZoneTilingJobRepository;
import app.bpartners.geojobs.repository.model.detection.FullDetection;
import app.bpartners.geojobs.repository.model.detection.ZoneDetectionJob;
import app.bpartners.geojobs.repository.model.tiling.ZoneTilingJob;
import app.bpartners.geojobs.service.detection.ZoneDetectionJobService;
import app.bpartners.geojobs.service.tiling.ZoneTilingJobService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class ZoneService {
  private static final Duration PRE_SIGNED_URL_DURATION = Duration.ofHours(1L);
  private final ZoneDetectionJobService zoneDetectionJobService;
  private final ZoneTilingJobService zoneTilingJobService;
  private final ZoneTilingJobMapper zoneTilingJobMapper;
  private final ZoneDetectionJobValidator detectionjobValidator;
  private final EventProducer eventProducer;
  private final TaskStatisticMapper taskStatisticMapper;
  private final ZoneDetectionJobRepository zoneDetectionJobRepository;
  private final FullDetectionRepository fullDetectionRepository;
  private final ZoneTilingJobRepository zoneTilingJobRepository;
  private final BucketComponent bucketComponent;

  public FullDetectedZone processTilingAndDetection(CreateFullDetection zoneToDetect) {
    String endToEndId = zoneToDetect.getEndToEndId();
    FullDetection fullDetection = fullDetectionRepository.findByEndToEndId(endToEndId);

    if (fullDetection.getZdjId() == null) {
      var ztj = processZoneTilingJob(zoneToDetect);
      return getTilingStat(fullDetection, ztj.getId());
    }

    String ZTJId = fullDetection.getZtjId();
    String ZDJId = fullDetection.getZdjId();
    Optional<ZoneTilingJob> zoneTilingJobOpt = zoneTilingJobRepository.findById(ZTJId);
    Optional<ZoneDetectionJob> zoneDetectionJobOpt = zoneDetectionJobRepository.findById(ZDJId);
    ZoneTilingJob zoneTilingJob = zoneTilingJobOpt.orElse(null);
    ZoneDetectionJob zoneDetectionJob = zoneDetectionJobOpt.orElse(null);

    assert zoneTilingJob != null;
    if (!zoneTilingJob.isSucceeded()) {
      return getTilingStat(fullDetection, ZTJId);
    }

    assert zoneDetectionJob != null;
    if (zoneDetectionJob.isPending() && zoneTilingJob.isSucceeded()) {
      processZoneDetectionJob(zoneToDetect, zoneTilingJob);
      return getDetectionStat(fullDetection, ZDJId);
    }
    return getDetectionStat(fullDetection, ZDJId);
  }

  private FullDetectedZone getTilingStat(FullDetection fullDetection, String ztjId) {
    return createFullDetectedZone(
        fullDetection.getGeojsonS3FileKey(),
        List.of(zoneTilingJobService.computeTaskStatistics(ztjId)),
        TILING);
  }

  private FullDetectedZone getDetectionStat(FullDetection fullDetection, String zdjId) {
    return createFullDetectedZone(
        fullDetection.getGeojsonS3FileKey(),
        List.of(zoneDetectionJobService.computeTaskStatistics(zdjId)),
        MACHINE_DETECTION);
  }

  private FullDetectedZone createFullDetectedZone(
      String s3FileKey, List<TaskStatistic> statistics, JobTypes jobType) {
    FullDetectedZone fullDetectedZone =
        new FullDetectedZone()
            .detectedGeojsonUrl(generatePresignedUrl(s3FileKey))
            .statistics(statistics.stream().map(taskStatisticMapper::toRest).toList());
    fullDetectedZone.setJobTypes(List.of(jobType));
    return fullDetectedZone;
  }

  private ZoneTilingJob processZoneTilingJob(CreateFullDetection zoneToDetect) {
    var createJob = zoneTilingJobMapper.from(zoneToDetect);
    var job = zoneTilingJobMapper.toDomain(createJob);
    String ZTJId = job.getId();
    var tilingTasks = getTilingTasks(createJob, ZTJId);
    return zoneTilingJobService.create(job, tilingTasks, zoneToDetect);
  }

  public void processZoneDetectionJob(CreateFullDetection zoneToDetect, ZoneTilingJob job) {
    String ZTJId = job.getId();
    ZoneDetectionJob zoneDetectionJob = getMachineZdjByZtjId(ZTJId);
    FullDetection fullDetection =
        fullDetectionRepository.findByEndToEndId(zoneToDetect.getEndToEndId());
    detectionjobValidator.accept(zoneDetectionJob.getId());
    DetectableObjectType detectableObjectType = zoneToDetect.getObjectType();
    if (detectableObjectType == null) {
      throw new ApiException(SERVER_EXCEPTION, "Object to detect is mandatory. ");
    }

    List<DetectableObjectConfiguration> detectableObjectConfigurations =
        List.of(
            new DetectableObjectConfiguration()
                .type(detectableObjectType)
                .confidence(zoneToDetect.getConfidence()));
    fullDetection.setDetectableObjectConfiguration(detectableObjectConfigurations.getFirst());
    fullDetectionRepository.save(fullDetection);
    ZoneDetectionJob processedZDJ =
        zoneDetectionJobService.processZDJ(
            zoneDetectionJob.getId(), detectableObjectConfigurations);

    if (!processedZDJ.isSucceeded()) {
      eventProducer.accept(List.of(new ZDJStatusRecomputingSubmitted(processedZDJ.getId())));
    }
  }

  private ZoneDetectionJob getMachineZdjByZtjId(String ztjId) {
    return zoneDetectionJobRepository.findAllByZoneTilingJob_Id(ztjId).stream()
        .filter(dJob -> MACHINE.equals(dJob.getDetectionType()))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "ZoneTilingJob(id="
                        + ztjId
                        + ") is not associated to any"
                        + " ZoneDetectionJob.type=MACHINE"));
  }

  private String generatePresignedUrl(String fileKey) {
    if (fileKey == null) {
      return null;
    }
    return bucketComponent.presign(fileKey, PRE_SIGNED_URL_DURATION).toString();
  }
}
